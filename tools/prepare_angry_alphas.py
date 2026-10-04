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
    # The upstream unrestricted @e selector scans ALL dimensions, including when
    # called via execute in. Override its tick entry point and dispatch once per
    # second with a distance filter, which restricts each scan to that dimension.
    with zipfile.ZipFile(out/"pokereport-angry-alphas-worlds.zip","w",zipfile.ZIP_DEFLATED) as z:
        z.writestr("pack.mcmeta",json.dumps({"pack":{"pack_format":48,"description":"PokeReport: Angry Alphas exploration-world dispatch"}}))
        z.writestr("data/minecraft/tags/function/tick.json",json.dumps({"values":["pokereport_angry_alphas:tick"]}))
        z.writestr("data/minecraft/tags/function/load.json",json.dumps({"values":["pokereport_angry_alphas:load"]}))
        z.writestr("data/pokereport_angry_alphas/function/load.mcfunction",
                   "scoreboard objectives add luna_alpha_clock dummy\nscoreboard players set #tick luna_alpha_clock 0\n")
        z.writestr("data/cmds/function/tick.mcfunction", "# Superseded by scoped once-per-second dispatch.\n")
        z.writestr("data/pokereport_angry_alphas/function/tick.mcfunction",
                   "scoreboard players add #tick luna_alpha_clock 1\n"
                   "execute if score #tick luna_alpha_clock matches 20.. run function pokereport_angry_alphas:scan\n")
        z.writestr("data/pokereport_angry_alphas/function/scan.mcfunction",
                   "scoreboard players set #tick luna_alpha_clock 0\n"+
                   "\n".join(f"execute in {world} as @e[type=cobblemon:pokemon,distance=0..,nbt={{Pokemon:{{Alpha:1b}}}}] at @s run function pokereport_angry_alphas:alpha" for world in ["minecraft:overworld"]+WORLDS)+"\n")
        z.writestr("data/pokereport_angry_alphas/function/alpha.mcfunction",
                   'execute unless data entity @s BattleId unless data entity @s Brain.memories."minecraft:angry_at".value if entity @p[distance=..10] run data modify entity @s Brain.memories."minecraft:angry_at".value set from entity @p[distance=..10] UUID\n'
                   'execute unless entity @a[distance=..10] run data remove entity @s Brain.memories."minecraft:angry_at"\n')
    print("Upstream hash verified; dimension-scoped alpha scans prepared (once per second).")
if __name__=="__main__":main()
