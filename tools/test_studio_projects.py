import copy,json,tempfile,unittest,shutil
from pathlib import Path
import compile_studio_projects as c
class StudioCompilerTest(unittest.TestCase):
 def setUp(self):self.temp=tempfile.TemporaryDirectory();self.folder=Path(self.temp.name)/'example';shutil.copytree(c.ROOT/'projects/example',self.folder);self.p=json.loads((self.folder/'project.json').read_text())
 def tearDown(self):self.temp.cleanup()
 def compile(self):(self.folder/'project.json').write_text(json.dumps(self.p));return c.compile_project(self.folder)
 def test_namespaced_assets_and_neutral_actor(self):
  p,assets=self.compile();self.assertTrue(all('motionrp_studio_example_'in a for a in assets));self.assertEqual(p['duration']/20,assets['players/motionrp_studio_example_body.bbmodel']['animations'][0]['length'])
 def test_paths_cannot_escape(self):self.p['models'][0]['file']='../outside.bbmodel';self.assertRaises(ValueError,self.compile)
 def test_unsupported_commands_and_overlap_are_rejected(self):
  self.p['events'][0]['type']='command';self.assertRaises(ValueError,self.compile)
 def test_particle_budget_is_bounded(self):
  self.p['events']=[dict(id='p'+str(i),type='particle',tick=0,endTick=20,particle='END_ROD',count=64,every=2)for i in range(3)];self.assertRaises(ValueError,self.compile)
 def test_external_textures_are_rejected(self):
  f=self.folder/'models/sword.bbmodel';m=json.loads(f.read_text());m['textures'][0]['source']='https://example.com/texture.png';f.write_text(json.dumps(m));self.assertRaises(ValueError,self.compile)
 def test_animation_script_is_rejected(self):
  f=self.folder/'models/sword.bbmodel';m=json.loads(f.read_text());m['animations']=[dict(name='run',length=1,animators={'effects':{'type':'effect','keyframes':[]}})];f.write_text(json.dumps(m));self.assertRaises(ValueError,self.compile)
if __name__=='__main__':unittest.main()
