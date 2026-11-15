from pathlib import Path
from PIL import Image
import unittest
RES=Path(__file__).resolve().parents[1]/'app/src/main/res'
class IconPixels(unittest.TestCase):
 def test_foreground_alpha_shape_and_safe_zone(self):
  for density,size in [('mdpi',108),('hdpi',162),('xhdpi',216),('xxhdpi',324),('xxxhdpi',432)]:
   a=Image.open(RES/f'mipmap-{density}/ic_launcher_foreground_light.png')
   b=Image.open(RES/f'mipmap-{density}/ic_launcher_foreground_dark.png')
   self.assertEqual(a.size,(size,size));self.assertEqual(a.mode,'RGBA')
   self.assertEqual(a.getchannel('A').tobytes(),b.getchannel('A').tobytes())
   self.assertEqual(a.getpixel((0,0))[3],0)
   count=0
   for y in range(size):
    for x in range(size):
     if a.getpixel((x,y))[3]>128:
      count+=1;dx=(x+.5)/size*108-54;dy=(y+.5)/size*108-54
      self.assertLess(dx*dx+dy*dy,33*33)
   self.assertGreater(count,size*size/14)
if __name__=='__main__':unittest.main(verbosity=2)
