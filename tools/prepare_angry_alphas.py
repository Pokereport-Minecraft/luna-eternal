"""Pin upstream Angry Alphas and add tick dispatch for PokeReport exploration worlds."""
import hashlib,json,urllib.request,zipfile
from pathlib import Path
ROOT=Path(__file__).resolve().parent.parent
URL="https://cdn.modrinth.com/data/ZMIGaD0V/versions/9chEWhXr/W%27s%20Angry%20Alphas.zip"
SHA1="d2b5f740966c7d010583afdf3c682955aadf9994"
WORLDS=["minecraft:the_nether","minecraft:the_end","lunaeternal:salvaje"]+[f"lunaeternal:salvaje{x}" for x in range(2,7)]+["lunaeternal:islas_naranja"]
def main():
    out=ROOT/"build/angry-alphas";out.mkdir(parents=True,exist_ok=True)
    data=urllib.request.urlopen(URL,timeout=30).read();assert hashlib.sha1(data).hexdigest()==SHA1
    upstream=out/"ws-angry-alphas-1.1.0.zip";upstream.write_bytes(data)
    with zipfile.ZipFile(upstream) as z:
        assert json.loads(z.read("pack.mcmeta"))["pack"]["pack_format"]==48
        assert "data/cmds/function/tick.mcfunction" in z.namelist()
    # Keep upstream byte-for-byte. Its own tick handles overworld; dispatch only other exploration worlds.
    with zipfile.ZipFile(out/"pokereport-angry-alphas-worlds.zip","w",zipfile.ZIP_DEFLATED) as z:
        z.writestr("pack.mcmeta",json.dumps({"pack":{"pack_format":48,"description":"PokeReport: Angry Alphas exploration-world dispatch"}}))
        z.writestr("data/minecraft/tags/function/tick.json",json.dumps({"values":["pokereport_angry_alphas:tick"]}))
        z.writestr("data/pokereport_angry_alphas/function/tick.mcfunction","\n".join(f"execute in {world} run function cmds:tick" for world in WORLDS)+"\n")
    print("Upstream hash verified; exploration dispatch prepared (9 additional dimensions).")
if __name__=="__main__":main()
