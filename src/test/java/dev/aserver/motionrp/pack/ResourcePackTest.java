package dev.aserver.motionrp.pack;

import com.google.gson.*;
import com.sun.net.httpserver.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import static org.junit.jupiter.api.Assertions.*;

class ResourcePackTest {
    @TempDir Path root;
    private PackSnapshot pack() throws Exception {
        Path path=root.resolve("build.zip");
        try(var zip=new ZipOutputStream(Files.newOutputStream(path))) {
            zip.putNextEntry(new ZipEntry("pack.mcmeta"));zip.write("{\"pack\":{\"pack_format\":75,\"description\":\"MotionRP\"}}".getBytes());zip.closeEntry();
            zip.putNextEntry(new ZipEntry("assets/bettermodel/test.json"));zip.write("{\"model\":\"test\"}".getBytes());zip.closeEntry();
        }
        return PackSnapshot.read(path);
    }
    @Test void snapshotsAreValidatedImmutableAndUseTheRequestedPublicFilename() throws Exception {
        var pack=pack();String hash=pack.sha256();byte[] external=pack.bytes();external[0]=0;
        assertEquals(hash,PackSnapshot.digest("SHA-256",pack.bytes()));
        assertEquals(40,pack.sha1().length());assertEquals(64,hash.length());
        assertTrue(pack.url("test/pack").endsWith("/"+pack.tag()+"/mortionRP.zip"));
        assertThrows(IllegalArgumentException.class,()->pack.url("../escape"));
        assertThrows(IllegalArgumentException.class,()->pack.url("test/pack?token=secret"));
    }
    @Test void truncatedOrUnrelatedArchivesCannotBePublished() throws Exception {
        pack();byte[] data=Files.readAllBytes(root.resolve("build.zip"));
        Files.write(root.resolve("broken.zip"),Arrays.copyOf(data,data.length-16));
        assertThrows(IOException.class,()->PackSnapshot.read(root.resolve("broken.zip")));
        try(var zip=new ZipOutputStream(Files.newOutputStream(root.resolve("other.zip")))) {
            zip.putNextEntry(new ZipEntry("other.txt"));zip.write(1);zip.closeEntry();
        }
        assertThrows(IOException.class,()->PackSnapshot.read(root.resolve("other.zip")));
    }
    @Test void uploadsDraftThenPublishesAndVerifiesAnonymousDownload() throws Exception {
        var pack=pack();try(var github=new FakeGitHub(pack);var client=github.client()) {
            client.publish("test/pack","test-token",pack);
            assertEquals(1,github.created);assertEquals(1,github.uploaded);assertEquals(1,github.published);
            assertArrayEquals(pack.bytes(),github.received);assertEquals(1,github.downloaded);
            assertFalse(github.authLeaked);assertTrue(github.apiAuthorized);
        }
    }
    @Test void anAlreadyPublishedHashIsReusedWithoutDeletingOrUploading() throws Exception {
        var pack=pack();try(var github=new FakeGitHub(pack);var client=github.client()) {
            github.exists=true;github.draft=false;github.hasAsset=true;
            client.publish("test/pack","test-token",pack);
            assertEquals(0,github.created);assertEquals(0,github.uploaded);assertEquals(0,github.published);
            assertEquals(1,github.downloaded);assertFalse(github.authLeaked);
        }
    }
    @Test void retriesResumeAnUploadedDraftWithoutDuplicateAssets() throws Exception {
        var pack=pack();try(var github=new FakeGitHub(pack);var client=github.client()) {
            github.exists=true;github.hasAsset=true;github.hideDraftTags=true;
            client.publish("test/pack","test-token",pack);
            assertEquals(0,github.created);assertEquals(0,github.uploaded);assertEquals(1,github.published);
        }
    }
    @Test void onlyAnUnfinishedZeroByteDraftAssetIsRemovedBeforeRetrying() throws Exception {
        var pack=pack();try(var github=new FakeGitHub(pack);var client=github.client()) {
            github.exists=true;github.hasAsset=true;github.partial=true;
            client.publish("test/pack","test-token",pack);
            assertEquals(1,github.deleted);assertEquals(1,github.uploaded);assertEquals(1,github.published);
        }
    }
    @Test void anInProgressDraftUploadIsNotDeletedByAnotherPublisher() throws Exception {
        var pack=pack();try(var github=new FakeGitHub(pack);var client=github.client()) {
            github.exists=true;github.hasAsset=true;github.partial=true;github.createdAt=java.time.Instant.now().toString();
            assertThrows(IOException.class,()->client.publish("test/pack","test-token",pack));
            assertEquals(0,github.deleted);assertEquals(0,github.uploaded);
        }
    }
    @Test void privateRepositoriesFailBeforePublishingAnything() throws Exception {
        var pack=pack();try(var github=new FakeGitHub(pack);var client=github.client()) {
            github.privateRepo=true;
            assertThrows(IOException.class,()->client.publish("test/pack","test-token",pack));
            assertEquals(0,github.created);assertEquals(0,github.uploaded);
        }
    }
    @Test void badCredentialsAreReportedByStatusCodeWithoutLeakingTheToken() throws Exception {
        var pack=pack();try(var github=new FakeGitHub(pack);var client=github.client()) {
            github.deny=true;
            var failure=assertThrows(GitHubPackPublisher.Failure.class,()->client.publish("test/pack","test-token",pack));
            assertTrue(failure.getMessage().contains("401"));assertFalse(failure.getMessage().contains("test-token"));
            assertEquals(0,github.uploaded);
        }
    }
    @Test void aTamperedExistingAssetIsKeptAndNeverOffered() throws Exception {
        var pack=pack();try(var github=new FakeGitHub(pack);var client=github.client()) {
            github.exists=true;github.hasAsset=true;github.badDigest=true;
            assertThrows(IOException.class,()->client.publish("test/pack","test-token",pack));
            assertEquals(0,github.uploaded);assertEquals(0,github.published);assertEquals(0,github.downloaded);
        }
    }
    @Test void aWrongPublicDownloadCannotBecomeTheActivePack() throws Exception {
        var pack=pack();try(var github=new FakeGitHub(pack);var client=github.client()) {
            github.corruptDownload=true;
            assertThrows(IOException.class,()->client.publish("test/pack","test-token",pack));assertFalse(github.authLeaked);
        }
    }
    @Test void apiRedirectsCannotForwardCredentials() throws Exception {
        var pack=pack();try(var github=new FakeGitHub(pack);var client=github.client()) {
            github.redirectApi=true;
            assertThrows(IOException.class,()->client.publish("test/pack","test-token",pack));assertEquals(0,github.stolen);
        }
    }
    private static final class FakeGitHub implements AutoCloseable {
        final HttpServer server;final URI base;final PackSnapshot pack;
        boolean partial,hideDraftTags,exists,draft=true,hasAsset,privateRepo,deny,badDigest,corruptDownload,redirectApi,authLeaked,apiAuthorized=true;
        int deleted,created,uploaded,published,downloaded,stolen;byte[] received;String createdAt="2000-01-01T00:00:00Z";
        FakeGitHub(PackSnapshot pack) throws IOException {
            this.pack=pack;server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
            base=URI.create("http://127.0.0.1:"+server.getAddress().getPort());server.createContext("/",this::handle);server.start();
        }
        GitHubPackPublisher client() { return new GitHubPackPublisher(base,base,base); }
        JsonObject asset() {
            var data=new JsonObject();data.addProperty("created_at",createdAt);data.addProperty("id",321);data.addProperty("state",partial?"starter":"uploaded");
            data.addProperty("name","mortionRP.zip");data.addProperty("size",partial?0:pack.bytes().length);
            data.addProperty("digest","sha256:"+(badDigest?"0".repeat(64):pack.sha256()));return data;
        }
        JsonObject release() {
            var data=new JsonObject();data.addProperty("tag_name",pack.tag());data.addProperty("id",123);data.addProperty("draft",draft);
            var assets=new JsonArray();if(hasAsset) assets.add(asset());data.add("assets",assets);return data;
        }
        void handle(HttpExchange exchange) throws IOException {
            String path=exchange.getRequestURI().getPath(),method=exchange.getRequestMethod();
            boolean publicRequest=path.startsWith("/test/pack/releases/download/");
            if(publicRequest || path.equals("/steal")) {
                if(exchange.getRequestHeaders().containsKey("Authorization")) authLeaked=true;
            } else apiAuthorized &= "Bearer test-token".equals(exchange.getRequestHeaders().getFirst("Authorization"));
            if(path.equals("/steal")) { stolen++;reply(exchange,200,"{}".getBytes());return; }
            if(publicRequest) {
                downloaded++;assertTrue(path.endsWith("/mortionRP.zip"));
                reply(exchange,200,corruptDownload?new byte[]{1,2,3}:pack.bytes());return;
            }
            if(redirectApi) { exchange.getResponseHeaders().set("Location",base+"/steal");reply(exchange,302,new byte[0]);return; }
            if(deny) { reply(exchange,401,"{\"message\":\"test-token must never be logged\"}".getBytes());return; }
            if(path.equals("/repos/test/pack")) { reply(exchange,200,("{\"private\":"+privateRepo+"}").getBytes());return; }
            if(path.contains("/releases/tags/")) { boolean visible=exists && !(hideDraftTags && draft);reply(exchange,visible?200:404,(visible?release().toString():"{}").getBytes());return; }
            if(path.equals("/repos/test/pack/releases") && method.equals("GET")) { reply(exchange,200,("["+release()+"]").getBytes());return; }
            if(path.equals("/repos/test/pack/releases") && method.equals("POST")) {
                if(exists) { reply(exchange,422,"{}".getBytes());return; }
                var body=JsonParser.parseString(new String(exchange.getRequestBody().readAllBytes())).getAsJsonObject();
                assertTrue(body.get("draft").getAsBoolean());assertTrue(body.get("prerelease").getAsBoolean());
                assertEquals("false",body.get("make_latest").getAsString());assertEquals(pack.tag(),body.get("tag_name").getAsString());
                exists=true;created++;reply(exchange,201,release().toString().getBytes());return;
            }
            if(path.equals("/repos/test/pack/releases/assets/321") && method.equals("DELETE")) {
                assertTrue(draft);assertTrue(partial);deleted++;hasAsset=false;partial=false;
                exchange.sendResponseHeaders(204,-1);exchange.close();return;
            }
            if(path.equals("/repos/test/pack/releases/123/assets") && method.equals("POST")) {
                assertEquals("name=mortionRP.zip",exchange.getRequestURI().getQuery());
                assertEquals("application/zip",exchange.getRequestHeaders().getFirst("Content-Type"));
                received=exchange.getRequestBody().readAllBytes();uploaded++;hasAsset=true;reply(exchange,201,asset().toString().getBytes());return;
            }
            if(path.equals("/repos/test/pack/releases/123") && method.equals("PATCH")) {
                draft=false;published++;reply(exchange,200,release().toString().getBytes());return;
            }
            reply(exchange,405,"{}".getBytes());
        }
        private static void reply(HttpExchange exchange,int status,byte[] body) throws IOException {
            exchange.sendResponseHeaders(status,body.length);try(var output=exchange.getResponseBody()) { output.write(body); }
        }
        @Override public void close() { server.stop(0); }
    }
}
