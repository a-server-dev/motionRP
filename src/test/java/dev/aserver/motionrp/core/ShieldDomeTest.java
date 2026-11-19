package dev.aserver.motionrp.core;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ShieldDomeTest {
 @Test void domeHasCurvatureAndFiniteOutwardNormals(){
  var patches=ShieldDome.patches(2);
  assertEquals(84,patches.size());
  double min=10,max=0;
  for(var p:patches){
   assertTrue(p.width()>0&&p.height()>0&&p.nz()>0);
   assertEquals(1,p.nx()*p.nx()+p.ny()*p.ny()+p.nz()*p.nz(),.00001);
   assertTrue(p.x()*p.x()+p.y()*p.y()<4);
   min=Math.min(min,p.z());max=Math.max(max,p.z());
  }
  assertTrue(max-min>.5);
  assertThrows(IllegalArgumentException.class,()->ShieldDome.patches(Double.NaN));
 }
}
