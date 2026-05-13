#!/usr/bin/env python3
"""
PP-OCRv3 ONNX バッチ全パイプライン検証スクリプト v3.1
- V1 style: axis-aligned crop + 20% padding (recognition quality maximized)
- Rotated detection box visualization (green) + recognition range visualization (magenta)
- Allowed character filtering + ground truth label matching
- 3-stage separation: Detection -> Classification -> Recognition
"""
import numpy as np
import cv2
import onnxruntime as ort
from pathlib import Path
import json, time, re, sys, csv

SCRIPT = Path(__file__).parent
BASE = SCRIPT.parent
MODELS = BASE / "models"
IMAGES = BASE / "images"
RESULTS = BASE / "results" / "batch_full_pipeline3"
RESULTS.mkdir(exist_ok=True)
LABEL_CSV = IMAGES / "正解ラベル.csv"
IMG_EXTS = {".jpg",".jpeg",".png",".JPG",".JPEG",".PNG"}
ALLOWED = set("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-+/._ ")

def filt(t):
    return "".join(c for c in t if c in ALLOWED)

def load_dd(p):
    with open(p,'r',encoding='utf-8') as f:
        return [l.strip() for l in f]

def nf(n):
    n = Path(n.strip()).name
    if not Path(n).suffix: n += ".jpg"
    if n[0]=='b' and len(n)>1 and n[1].isdigit(): n=n[1:]
    if n.startswith('II') and not n.startswith('III'): n=n[1:]
    return n.lower()

def labels(cp, ifs):
    im = {f.name.lower():f.name for f in ifs}
    lb = {}
    with open(cp,'r',encoding='utf-8') as f:
        rd = csv.reader(f); next(rd)
        for r in rd:
            if len(r)<8: continue
            co = [c.strip() for c in r[7].strip().split('\n') if c.strip()]
            if not co: continue
            nm = nf(r[1])
            if nm in im: lb[im[nm]] = co
            else:
                st = Path(nm).stem
                for ln,on in im.items():
                    if Path(ln).stem==st:
                        lb[on]=co; break
                else: print(f"  NOLABEL: '{r[1].strip()}'->'{nm}'")
    return lb

def dpre(img):
    h,w = img.shape[:2]
    s = min(960/h,960/w)
    nh = max(32,int(h*s)//32*32)
    nw = max(32,int(w*s)//32*32)
    r = cv2.resize(img,(nw,nh))
    b = cv2.dnn.blobFromImage(r,1.0,(nw,nh),(123.675,116.28,103.53),swapRB=True)
    m = np.array([0.485,0.456,0.406]).reshape(1,3,1,1)
    sd = np.array([0.229,0.224,0.225]).reshape(1,3,1,1)
    return ((b/255.0-m)/sd).astype(np.float32),s,(h,w)

def dpost(out,ohw,dt=0.3):
    pr = out[0,0]
    bi = (pr>dt).astype(np.uint8)
    cs,_ = cv2.findContours(bi,cv2.RETR_LIST,cv2.CHAIN_APPROX_SIMPLE)
    ph,pw = pr.shape; oh,ow = ohw; sx,sy = ow/pw,oh/ph
    bx,ag,sc = [],[],[]
    for c in cs:
        if len(c)<4: continue
        rt = cv2.minAreaRect(c); bo=cv2.boxPoints(rt)
        if rt[1][0]<5 or rt[1][1]<5: continue
        ms = np.zeros((ph,pw),dtype=np.uint8)
        cc=c.copy(); cc[:,:,0]=np.clip(cc[:,:,0]/sx,0,pw-1).astype(np.int32)
        cc[:,:,1]=np.clip(cc[:,:,1]/sy,0,ph-1).astype(np.int32)
        cv2.fillPoly(ms,[cc],[255])
        sf = float(np.mean(pr[ms>0])) if np.sum(ms)>0 else 0.0
        bo[:,0]*=sx; bo[:,1]*=sy; bo=bo.astype(np.int32)
        bo[:,0]=np.clip(bo[:,0],0,ow-1); bo[:,1]=np.clip(bo[:,1],0,oh-1)
        bx.append(bo); ag.append(rt[2]); sc.append(sf)
    return bx,ag,sc

def rpre(cr):
    rh,rw=48,320; h,w=cr.shape[:2]; ra=w/h
    wr = min(rw,max(1,int(ra*rh)))
    im = cv2.resize(cr,(wr,rh)).astype(np.float32)
    im = (im-127.5)/127.5
    pa = np.full((rh,rw,3),-1.0,dtype=np.float32); pa[:,:wr,:]=im
    return np.transpose(pa,(2,0,1))[np.newaxis,:,:,:].copy()

def ctc(lo,dd):
    ps=np.argmax(lo,axis=-1); rs,cf=[],[]
    for b in range(ps.shape[0]):
        sq=ps[b]; r,p=[],-1; cfs=[]
        for t in range(len(sq)):
            c=sq[t]
            if c==0: p=-1; continue
            if c==p: continue
            i=c-1
            if 0<=i<len(dd): r.append(dd[i]); cfs.append(float(np.exp(lo[b,t,c])/np.sum(np.exp(lo[b,t]))))
            else: r.append('?'); cfs.append(0.0)
            p = c
        rs.append(''.join(r)); cf.append(np.mean(cfs) if cfs else 0.0)
    return rs,cf

def recog(cr,ss,dd):
    bl=rpre(cr); o=ss.run([ss.get_outputs()[0].name],{ss.get_inputs()[0].name:bl})[0]
    t,c=ctc(o,dd); return (t[0] if t else ""),(c[0] if c else 0.0)

def eprod(t):
    return [m for m in re.findall(r'[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*',t.upper()) if len(m)>=3]

def lrv(s1,s2):
    if not s1 and not s2: return 1.0
    if not s1 or not s2: return 0.0
    ml=max(len(s1),len(s2))
    if ml==0: return 1.0
    m,n=len(s1),len(s2); dp=list(range(n+1))
    for i in range(1,m+1):
        pv=dp[0];dp[0]=i
        for j in range(1,n+1):
            tp=dp[j];dp[j]=pv if s1[i-1]==s2[j-1] else 1+min(pv,dp[j],dp[j-1]);pv=tp
    return 1.0-dp[n]/ml

def mtc(rc,co):
    if not co:
        return {"em":[],"pm":[],"ms":[],"ex":[],"er":0.0,"pr":0.0}
    rn=[c.strip().upper() for c in rc]; cn=[c.strip().upper() for c in co]
    em,pm,mr,mc=[],[],set(),set()
    for i,r in enumerate(rn):
        for j,c in enumerate(cn):
            if j in mc: continue
            if r==c: em.append({"recogni":rc[i],"corr":co[j]});mr.add(i);mc.add(j);break
    for i,r in enumerate(rn):
        if i in mr: continue
        for j,c in enumerate(cn):
            if j in mc: continue
            if r in c or c in r or lrv(r,c)>=0.7:
                pm.append({"recogni":rc[i],"corr":co[j]});mr.add(i);mc.add(j);break
    ms=[co[j] for j in range(len(co)) if j not in mc]
    xs=[rc[i] for i in range(len(rc)) if i not in mr]
    tc=len(co)
    return {"exact_matches":em,"partial_matches":pm,"misses":ms,"extra":xs,
            "exact_rate":round(len(em)/tc,4) if tc else 0.0,
            "partial_rate":round((len(em)+len(pm))/tc,4) if tc else 0.0}

def proc(ip,ds,rs,dd):
    img=cv2.imread(str(ip),cv2.IMREAD_COLOR)
    if img is None: print(f"  FAIL: {ip}"); return None
    oh,ow=img.shape[:2]
    # Stage 1: Detection
    di,sc,ohw=dpre(img)
    dr=ds.run([ds.get_outputs()[0].name],{ds.get_inputs()[0].name:di})[0]
    boxes,angles,scores=dpost(dr,ohw,0.3)
    # Stage 2: Classification
    ai=[]
    for i,(bo,an) in enumerate(zip(boxes,angles)):
        ai.append({"idx":i,"angle":an,"cx":int(bo[:,0].mean()),"cy":int(bo[:,1].mean())})
    # Compute expanded boxes (same for recognition)
    eboxes=[]
    for bo in boxes:
        xm,ym=bo[:,0].min(),bo[:,1].min(); xx,yx=bo[:,0].max(),bo[:,1].max()
        pw=int((xx-xm)*0.4);ph=int((yx-ym)*0.4)
        eboxes.append(np.array([[max(0,xm-pw),max(0,ym-ph)],[min(ow-1,xx+pw),max(0,ym-ph)],
                                [min(ow-1,xx+pw),min(oh-1,yx+ph)],[max(0,xm-pw),min(oh-1,yx+ph)]],dtype=np.int32))
    # Stage 3: Recognition (axis-aligned crop with 40% pad)
    dets=[]
    for i,bo in enumerate(boxes):
        xm,ym=bo[:,0].min(),bo[:,1].min(); xx,yx=bo[:,0].max(),bo[:,1].max()
        pw=int((xx-xm)*0.4);ph=int((yx-ym)*0.4)
        cr=img[max(0,ym-ph):min(oh,yx+ph),max(0,xm-pw):min(ow,xx+pw)]
        tx,co=recog(cr,rs,dd); tf=filt(tx); pr=eprod(tf)
        dets.append({"box_index":i,"angle_degree":round(float(angles[i]),2),
            "det_score":round(float(scores[i]),4),"box_points":bo.tolist(),
            "recognized_text":tx,"text_filtered":tf,"confidence":round(co,4),
            "product_codes":pr,"expanded_box":eboxes[i].tolist()})
    ap=[p for d in dets for p in d["product_codes"]]
    # Draw
    im=img.copy()
    for i,de in enumerate(dets):
        bo=np.array(de["box_points"]); eb=np.array(de["expanded_box"])
        cv2.polylines(im,[bo],True,(0,255,0),2)
        cv2.polylines(im,[eb],True,(255,0,255),2)
        cx=int(bo[:,0].mean()); ly=int(bo[:,1].min())-5
        cv2.putText(im,f"{de['angle_degree']:.1f}|{de['text_filtered']}",(cx-120,ly),
            cv2.FONT_HERSHEY_SIMPLEX,3.0,(0,0,255),4)
    cv2.imwrite(str(RESULTS/f"{ip.stem}_full_pipeline.jpg"),im)
    with open(RESULTS/f"{ip.stem}_full_pipeline.json",'w',encoding='utf-8') as f:
        json.dump({"image":ip.name,"image_size":[ow,oh],"num_detections":len(dets),
            "detection_model":"det.onnx","recognition_model":"en_PP-OCRv3_rec.onnx",
            "stages":{"s1_d":{"model":"det.onnx","desc":"テキスト領域検出","num":len(boxes)},
                "s2_c":{"model":"minAreaRect","desc":"角度抽出","info":ai},
                "s3_r":{"model":"en_PP-OCRv3_rec.onnx","desc":"axis-aligned crop+20%pad","dets":dets}},
            "all_product_codes":ap},f,indent=2,ensure_ascii=False)
    return {"all_product_codes":ap,"n":len(dets)}

def main():
    ifs=sorted(f for f in IMAGES.iterdir() if f.is_file() and f.suffix in IMG_EXTS)
    print(f"Found {len(ifs)} in {IMAGES}")
    lb=labels(LABEL_CSV,ifs); print(f"  Labels: {len(lb)}")
    if not ifs: sys.exit(1)
    ds=ort.InferenceSession(str(MODELS/"det.onnx"))
    rs=ort.InferenceSession(str(MODELS/"en_PP-OCRv3_rec.onnx"))
    dd=load_dd(MODELS/"en_PP-OCRv3_dict.txt")
    print("Models loaded.")
    sm=[];t0=time.time();er=0;pr=0;nl=0
    for i,ip in enumerate(ifs,1):
        print(f"\n[{i}/{len(ifs)}] {ip.name}"); t1=time.time(); r=proc(ip,ds,rs,dd); el=time.time()-t1
        if r is None: sm.append({"image":ip.name,"status":"FAIL","dets":0,"time":round(el,2)}); continue
        cc=lb.get(ip.name,[]); m=mtc(r["all_product_codes"],cc) if cc else None
        if m: er+=m["exact_rate"]; pr+=m["partial_rate"]; nl+=1
        print(f"  -> {r['n']} dets, prods: {r['all_product_codes']}, {el:.2f}s")
        if m: print(f"     Exact: {m['exact_rate']:.0%} ({len(m['exact_matches'])}/{len(cc)}), Part: {m['partial_rate']:.0%}")
        if m and m['misses']: print(f"     Miss: {m['misses']}")
        if m and m['extra']: print(f"     Extra: {m['extra']}")
        e={"image":ip.name,"status":"OK","dets":r["n"],"prods":r["all_product_codes"],"correct":cc,"time":round(el,2)}
        if m: e["match"]=m; sm.append(e)
    te=time.time()-t0; ae=er/nl if nl else 0; ap2=pr/nl if nl else 0
    with open(RESULTS/"_summary.json",'w',encoding='utf-8') as f:
        json.dump({"total":len(ifs),"labeled":nl,"time":round(te,2),"avg_exact":round(ae,4),"avg_partial":round(ap2,4),"results":sm},f,indent=2,ensure_ascii=False)
    print(f"\n{'='*70}\nBATCH v3.1: V1-style axis-aligned crop + 40% padding\n{'='*70}")
    print(f"Images: {len(ifs)} | Labeled: {nl} | Time: {te:.2f}s | Exact: {ae:.1%} | Part: {ap2:.1%}")
    print(f"{'='*70}")
    for s in sm:
        ms=f" | exact={s['match']['exact_rate']:.0%} part={s['match']['partial_rate']:.0%}" if 'match' in s else ''
        print(f"  {s['image']:45s} | {s['status']} | {s['dets']:3d} | {s['prods']}{ms} | {s['time']:.2f}s")
    print(f"{'='*70}")

if __name__=="__main__": main()
