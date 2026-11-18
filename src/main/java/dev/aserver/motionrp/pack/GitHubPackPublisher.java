package dev.aserver.motionrp.pack;

import com.google.gson.*;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

/** Immutable hash-tagged releases; credentials go only to GitHub's API/upload hosts. */
public final class GitHubPackPublisher implements AutoCloseable {
    private final HttpClient api=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).followRedirects(HttpClient.Redirect.NEVER).build();
    private final HttpClient downloads=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).followRedirects(HttpClient.Redirect.NORMAL).build();
    private final URI apiBase,uploadBase,publicBase;
    public GitHubPackPublisher() { this(URI.create("https://api.github.com"),URI.create("https://uploads.github.com"),URI.create("https://github.com")); }
    GitHubPackPublisher(URI apiBase,URI uploadBase,URI publicBase) { this.apiBase=apiBase;this.uploadBase=uploadBase;this.publicBase=publicBase; }
    public static final class Failure extends IOException {
        Failure(String message) { super(message); }
    }
    private record Response(int status,JsonObject body) {}
    public void publish(String repository,String token,PackSnapshot pack) throws IOException,InterruptedException {
        repository=PackSnapshot.repository(repository);
        String root="/repos/"+repository;
        Response repo=request("GET",apiBase.resolve(root),token,null,"application/json");require(repo,200);
        if(!repo.body.has("private") || repo.body.get("private").getAsBoolean()) throw new Failure("Resource pack repository must be public");
        Response found=request("GET",apiBase.resolve(root+"/releases/tags/"+pack.tag()),token,null,"application/json");
        if(found.status==404) {
            var create=new JsonObject();create.addProperty("tag_name",pack.tag());create.addProperty("name","MotionRP resource pack "+pack.sha256().substring(0,12));
            create.addProperty("body","Automatically generated server resource pack.\nSHA-1: "+pack.sha1()+"\nSHA-256: "+pack.sha256());
            create.addProperty("draft",true);create.addProperty("prerelease",true);create.addProperty("make_latest","false");
            found=request("POST",apiBase.resolve(root+"/releases"),token,json(create),"application/json");
            if(found.status==422) {
                found=request("GET",apiBase.resolve(root+"/releases/tags/"+pack.tag()),token,null,"application/json");
                if(found.status==404) {
                    // GitHub's by-tag endpoint can exclude drafts, even with authentication.
                    Response list=request("GET",apiBase.resolve(root+"/releases?per_page=100"),token,null,"application/json");require(list,200);
                    for(var entry:list.body.getAsJsonArray("items")) {
                        var release=entry.getAsJsonObject();
                        if(release.get("tag_name").getAsString().equals(pack.tag())) { found=new Response(200,release);break; }
                    }
                }
            } else require(found,201);
        }
        require(found,200,201);long id=found.body.get("id").getAsLong();JsonObject asset=null;
        for(var entry:found.body.getAsJsonArray("assets")) if(entry.getAsJsonObject().get("name").getAsString().equals("mortionRP.zip")) asset=entry.getAsJsonObject();
        if(asset!=null && found.body.get("draft").getAsBoolean() && asset.has("state")
                && asset.get("state").getAsString().equals("starter") && asset.get("size").getAsLong()==0
                && asset.has("created_at") && Instant.parse(asset.get("created_at").getAsString()).isBefore(Instant.now().minusSeconds(120))) {
            // GitHub can leave a zero-byte starter after a failed upload. Only
            // that unfinished draft asset is removed; published assets are immutable.
            require(request("DELETE",apiBase.resolve(root+"/releases/assets/"+asset.get("id").getAsLong()),token,null,"application/json"),204);
            asset=null;
        }
        if(asset==null) {
            Response upload=request("POST",uploadBase.resolve(root+"/releases/"+id+"/assets?name=mortionRP.zip"),token,pack.bytes(),"application/zip");
            require(upload,201);asset=upload.body;
        }
        if(!asset.get("name").getAsString().equals("mortionRP.zip") || asset.get("size").getAsLong()!=pack.bytes().length)
            throw new Failure("GitHub asset size/name does not match the generated pack");
        if(asset.has("digest") && !asset.get("digest").isJsonNull() && !asset.get("digest").getAsString().equals("sha256:"+pack.sha256()))
            throw new Failure("GitHub asset digest does not match; existing asset was kept");
        if(found.body.get("draft").getAsBoolean()) {
            var publish=new JsonObject();publish.addProperty("draft",false);publish.addProperty("prerelease",true);publish.addProperty("make_latest","false");
            require(request("PATCH",apiBase.resolve(root+"/releases/"+id),token,json(publish),"application/json"),200);
        }
        // Verify the anonymous URL that Minecraft will use. Never attach the token here.
        URI download=publicBase.resolve("/"+repository+"/releases/download/"+pack.tag()+"/mortionRP.zip");
        var response=downloads.send(HttpRequest.newBuilder(download).timeout(Duration.ofSeconds(120)).GET().build(),
                HttpResponse.BodyHandlers.limiting(HttpResponse.BodyHandlers.ofByteArray(),PackSnapshot.MAX_BYTES));
        if(response.statusCode()!=200) throw new Failure("Public pack download returned HTTP "+response.statusCode());
        if(!PackSnapshot.digest("SHA-256",response.body()).equals(pack.sha256()))
            throw new Failure("Public resource pack download failed hash verification");
    }
    private static byte[] json(JsonObject object) { return object.toString().getBytes(StandardCharsets.UTF_8); }
    private Response request(String method,URI uri,String token,byte[] data,String type) throws IOException,InterruptedException {
        var builder=HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(120)).header("Authorization","Bearer "+token)
                .header("Accept","application/vnd.github+json").header("X-GitHub-Api-Version","2022-11-28").header("User-Agent","MotionRP-resource-pack")
                .header("Content-Type",type).method(method,data==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofByteArray(data));
        var response=api.send(builder.build(),HttpResponse.BodyHandlers.limiting(HttpResponse.BodyHandlers.ofByteArray(),4*1024*1024));
        byte[] body=response.body();JsonObject result=new JsonObject();
        if(response.statusCode()>=200 && response.statusCode()<300 && body.length>0) {
            try {
                var parsed=JsonParser.parseString(new String(body,StandardCharsets.UTF_8));
                if(parsed.isJsonArray()) result.add("items",parsed);else result=parsed.getAsJsonObject();
            } catch(RuntimeException invalid) { throw new IOException("Invalid GitHub API response"); }
        }
        return new Response(response.statusCode(),result);
    }
    private static void require(Response response,int... allowed) throws IOException {
        for(int code:allowed) if(response.status==code) return;
        // Do not log response bodies, headers or credentials.
        throw new Failure("GitHub API returned HTTP "+response.status);
    }
    @Override public void close() { api.shutdownNow();downloads.shutdownNow(); }
}
