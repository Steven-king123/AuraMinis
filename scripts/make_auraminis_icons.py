from PIL import Image,ImageDraw,ImageFilter,ImageFont
from pathlib import Path
import json,hashlib,math,shutil
r=Path(__file__).resolve().parents[1];res=r/'src/android/app/src/main/res';out=r/'assets/brand/android';out.mkdir(parents=True,exist_ok=True)
pair=Image.open(r/'assets/brand/approved-icon-pair.png').convert('RGB')
# Trace the approved silhouette, not a redesigned typeset A. Coordinates in the original light tile.
commands=[('M',(144,660)),('L',(298,289)),('C',(316,245,329,237,375,236)),('C',(406,233,447,231,468,248)),('C',(482,260,491,279,501,302)),('L',(647,647)),('C',(658,674,655,693,639,704)),('C',(623,715,564,716,548,705)),('C',(530,692,525,660,505,638)),('C',(463,605,409,583,351,571)),('C',(332,601,309,658,288,689)),('C',(270,714,245,715,204,714)),('C',(161,714,136,699,144,660)),('Z',()),('M',(404,350)),('L',(335,527)),('C',(368,511,392,495,420,482)),('C',(432,477,443,477,455,477)),('L',(404,350)),('Z',())]
def points(cmds):
 paths=[];p=[];cur=(0,0)
 for typ,v in cmds:
  if typ=='M':p=[v];cur=v
  elif typ=='L':p.append(v);cur=v
  elif typ=='C':
   a=cur;b=v[:2];c=v[2:4];d=v[4:]
   for i in range(1,51):
    t=i/50;u=1-t;p.append((u**3*a[0]+3*u*u*t*b[0]+3*u*t*t*c[0]+t**3*d[0],u**3*a[1]+3*u*u*t*b[1]+3*u*t*t*c[1]+t**3*d[1]))
   cur=d
  elif typ=='Z':paths.append(p)
 return paths
paths=points(commands)
# At most 30.5dp from the center, inside the 33dp guaranteed adaptive-icon safe circle.
cx,cy=398,475;radius=max(math.hypot(x-cx,y-cy) for p in paths for x,y in p);scale=30.5/radius
S=864
mask=Image.new('L',(S,S));d=ImageDraw.Draw(mask)
for i,p in enumerate(paths):d.polygon([((54+(x-cx)*scale)*8,(54+(y-cy)*scale)*8) for x,y in p],fill=255 if i==0 else 0)
mask=mask.filter(ImageFilter.GaussianBlur(.65))
masters={}
for theme,dx,bgc in [('light',0,(243,245,250)),('dark',745,(12,19,44))]:
 # Resample exactly the corresponding approved ribbon, with no baked tile/corner around it.
 fg=pair.transform((S,S),Image.Transform.AFFINE,(1/(8*scale),0,cx+dx-54/scale,0,1/(8*scale),cy-54/scale),Image.Resampling.BICUBIC).convert('RGBA');fg.putalpha(mask)
 bg=Image.new('RGB',(S,S),bgc)
 # Retain only a soft aura in the separate full-bleed background layer.
 glow=Image.new('RGBA',(S,S));gd=ImageDraw.Draw(glow)
 gd.ellipse((S*.18,S*.22,S*.75,S*.91),fill=(42,145,242,28 if theme=='dark' else 10))
 gd.ellipse((S*.37,S*.24,S*.91,S*.92),fill=(145,85,234,28 if theme=='dark' else 9))
 bg=Image.alpha_composite(bg.convert('RGBA'),glow.filter(ImageFilter.GaussianBlur(90))).convert('RGB')
 fg.save(out/f'foreground-{theme}.png');bg.save(out/f'background-{theme}.png');masters[theme]=(fg,bg)
for density,factor in [('mdpi',1),('hdpi',1.5),('xhdpi',2),('xxhdpi',3),('xxxhdpi',4)]:
 for theme in ['light','dark']:
  fg,bg=masters[theme];target=res/f'mipmap-{density}';night=res/f'mipmap-night-{density}'
  target.mkdir(exist_ok=True);night.mkdir(exist_ok=True)
  fg.resize((round(108*factor),)*2,Image.Resampling.LANCZOS).save(target/f'ic_launcher_foreground_{theme}.png')
  if theme=='light':fg.resize((round(108*factor),)*2,Image.Resampling.LANCZOS).save(target/'ic_launcher_foreground.png')
  else:fg.resize((round(108*factor),)*2,Image.Resampling.LANCZOS).save(night/'ic_launcher_foreground.png')
  # Legacy square 48dp launcher fallback; system mask is not baked into the artwork.
  composite=Image.alpha_composite(bg.convert('RGBA'),fg).resize((round(48*factor),)*2,Image.Resampling.LANCZOS)
  composite.save((target if theme=='light' else night)/'ic_launcher.png')
# Separate background bitmap for full-bleed adaptive icons; no shape baked in.
drawable=res/'drawable-nodpi';drawable.mkdir(exist_ok=True)
for t,(fg,bg) in masters.items():bg.save(drawable/f'ic_launcher_background_{t}.png')
# Resource files edited by the applying step; expose a compact vector silhouette for themed icons.
parts=[]
for typ,v in commands:
 if typ in ['M','L']:parts.append(typ+','.join(f'{54+(v[i]-(cx if i==0 else cy))*scale:.3f}' for i in range(2)))
 elif typ=='C':parts.append('C'+','.join(f'{54+(v[i]-(cx if i%2==0 else cy))*scale:.3f}' for i in range(6)))
 else:parts.append('Z')
(out/'monochrome-path.txt').write_text(' '.join(parts))
# Review at circle/squircle masks with 48px launcher examples.
sheet=Image.new('RGB',(1200,700),(226,231,240));sd=ImageDraw.Draw(sheet)
font=str(r/'src/ios/Resources/KaTeX/fonts/KaTeX_SansSerif-Regular.ttf')
sd.text((44,24),'AuraMinis / Android adaptive icon proof',font=ImageFont.truetype(font,31),fill=(28,38,62))
for j,t in enumerate(['light','dark']):
 fg,bg=masters[t];im=Image.alpha_composite(bg.convert('RGBA'),fg)
 for k,shape in enumerate(['circle','squircle']):
  size=250;m=Image.new('L',(size,size));md=ImageDraw.Draw(m)
  if shape=='circle':md.ellipse((0,0,size-1,size-1),fill=255)
  else:md.rounded_rectangle((0,0,size-1,size-1),radius=57,fill=255)
  tile=im.resize((size,size),Image.Resampling.LANCZOS);sheet.paste(tile,(50+j*600+k*280,120),m)
  sd.text((50+j*600+k*280,388),t+' / '+shape,font=ImageFont.truetype(font,21),fill=(50,65,90))
 for k,size in enumerate([96,64,48]):
  tile=im.resize((size,size),Image.Resampling.LANCZOS);m=Image.new('L',(size,size));ImageDraw.Draw(m).rounded_rectangle((0,0,size-1,size-1),radius=round(size*.23),fill=255)
  sheet.paste(tile,(70+j*600+k*155,482+(96-size)//2),m)
  sd.text((70+j*600+k*155,602),str(size)+'px',font=ImageFont.truetype(font,19),fill=(60,73,94))
sheet.save(out/'adaptive-proof.png')
print(json.dumps({'master_size':S,'max_radius_dp':30.5,'source':'approved AI pair, traced outline','proof':str(out/'adaptive-proof.png')},indent=2))
