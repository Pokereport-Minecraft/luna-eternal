"""One-time, explicitly authorized player wipe. Run on node after coordinated shutdown/backup.
Keeps schema, server catalogs, terrain and NPCs. Fails closed with server still stopped.
"""
import pathlib, subprocess, json, sqlite3, hashlib, shutil, io, gzip, zlib, uuid, time, collections
import nbtlib
UUID="1f23bff0-bc4c-4c00-bbcf-2fe81ff666dd"
ROOT=pathlib.Path("/var/lib/pterodactyl/volumes")/UUID
DB="s11945_luna"
PRESERVE={"schema_version","economy_control_state","hunt_cycle","hunt_target","kit_rotacion","pase_temporada","placed_ore","santuario"}
EXPECTED=set("backpack_slot black_market_offer black_market_purchase card_ability card_pack_claim card_pack_grant clan clan_home clan_invite clan_ledger clan_log clan_member crate_key crate_open crate_pity crianza_entrega_pendiente crianza_ranura economic_session economy_control_state economy_excluded_player economy_snapshot exclusive_kit_purchase gts_listing gym_badge hunt_cycle hunt_progress hunt_target kit_claim kit_rotacion ledger_entry legacy_mark_balance lunar_stop_delivery lunar_stop_progress lunita_player_memory market_claim market_order market_trade npc_trade_daily pase_jugador pase_reclamo pase_temporada placed_ore player player_activity player_cosmetic_equipped player_cosmetics player_economy player_homes player_job player_path player_puerta player_suit player_suit_owned pokedex_entry pokevoice_preferences pokevoice_relationship pwarp_favorite pwarp_recent quest_progress santuario santuario_foto santuario_honor santuario_honor_click santuario_premio santuario_visita schema_version tebex_fulfillment tebex_fulfillment_effect tebex_payment world_return".split())
def sql(q):return subprocess.check_output(["mariadb","-N",DB,"-e",q],text=True).strip()
def safe(relative):
    path=ROOT/relative
    assert path.resolve().is_relative_to(ROOT.resolve()) and path.resolve()!=ROOT.resolve()
    return path

def main():
    assert subprocess.check_output(["docker","inspect","-f","{{.State.Status}}",UUID]).strip()==b"exited"
    info=json.loads(pathlib.Path("/tmp/player-reset-backup.json").read_text());backup=pathlib.Path(info["backup"])
    assert backup.is_relative_to(pathlib.Path("/var/backups/pokereport/player-reset"))
    previous=pathlib.Path("/tmp/player-reset-report.json")
    if previous.exists():
        old_report=json.loads(previous.read_text())
        assert old_report.get("backup")!=str(backup), "This backup already completed a wipe; do not reuse it"
    assert (backup/"affected-files.tar").stat().st_size>1000000 and (backup/"database.sql").stat().st_size>1000
    assert json.loads(pathlib.Path("/tmp/player-reset-tebex-check.json").read_text())["commands"]==0
    tables=set(sql("SHOW TABLES").splitlines());assert tables==EXPECTED,"Unclassified database tables; stop before deletion"
    before={t:int(sql("SELECT COUNT(*) FROM `"+t+"`")) for t in tables}
    identities=sql("SELECT mc_uuid,username FROM player").splitlines()
    ids={row.split("\t")[0] for row in identities};names={row.split("\t")[1] for row in identities}
    for rel in ["usercache.json","ops.json","whitelist.json","banned-players.json"]:
        for record in json.loads(safe(rel).read_text()):
            if record.get("uuid"):ids.add(record["uuid"])
            if record.get("name"):names.add(record["name"])
    for p in safe("world/playerdata").glob("*.dat"):
        try:ids.add(str(uuid.UUID(p.stem)))
        except ValueError:pass
    ids={str(uuid.UUID(x)) for x in ids}
    def is_old(value):
        if isinstance(value,nbtlib.IntArray) and len(value)==4:
            raw=b"".join((int(x)&0xffffffff).to_bytes(4,"big") for x in value)
            return str(uuid.UUID(bytes=raw)) in ids
        return isinstance(value,str) and value in ids
    def owned(entity):
        for key,value in entity.items():
            if "owner" in key.lower() and is_old(value):return True
        pokemon=entity.get("Pokemon",{})
        tags={str(x) for x in entity.get("Tags",[])}
        if str(entity.get("id"))=="cobblemon:pokemon" and "luna_decorativo" not in tags:
            for key,value in pokemon.items():
                if ("owner" in key.lower() or "originaltrainer" in key.lower()) and is_old(value):return True
        return False
    # Validate all region headers/payloads before any files move; final sectors need not be padded.
    for region in safe("world").rglob("*.mca"):
        if region.parent.name != "entities":continue
        data=region.read_bytes()
        if not data:continue
        assert len(data)>=8192, "Truncated region header: " + str(region)
        for index in range(1024):
            off=int.from_bytes(data[index*4:index*4+3],"big")*4096
            if not off:continue
            assert off+5<=len(data), "Bad region offset"
            length=int.from_bytes(data[off:off+4],"big")
            assert length>0 and off+4+length<=len(data), "Truncated region payload"
            assert data[off+4] in (1,2,3), "Unsupported region compression"
    # Personal files and live copies of old log/permission backups move OUTSIDE the server.
    moved=[]
    paths=["world/playerdata","world/stats","world/advancements","world/pokemon","world/pokedex","world/cobblemonplayerdata","world/skinrestorer","world/cobblenav","world/tm_moves","world/claimblocks","world/lunaeternal/market-receipts","config/lunaeternal/fotos",".staff-permission-backups","logs","crash-reports","EasyAuth"]
    paths += [p.relative_to(ROOT).as_posix() for p in safe("world/data").glob("rctmod.player.*")]
    paths += [p.relative_to(ROOT).as_posix() for p in safe("world/data").glob("rctmod.trainers.*.mem.dat")]
    for rel in paths:
        p=safe(rel)
        if p.exists():
            target=backup/"retired-live-files"/rel;target.parent.mkdir(parents=True,exist_ok=True)
            shutil.move(str(p),str(target));moved.append(rel)
    # Recreate only the SQLite schema; no old passwords/IPs/session fields survive.
    old=sqlite3.connect(backup/"easyauth.db");new_path=safe("EasyAuth/easyauth.db");new_path.parent.mkdir(parents=True,exist_ok=True)
    new=sqlite3.connect(new_path)
    for (ddl,) in old.execute("SELECT sql FROM sqlite_master WHERE sql IS NOT NULL AND name NOT LIKE 'sqlite_%' ORDER BY type='table' DESC"):
        new.execute(ddl)
    new.commit();assert new.execute("SELECT COUNT(*) FROM easyauth").fetchone()[0]==0;new.close();old.close()
    for rel in ["ops.json","whitelist.json","banned-players.json","banned-ips.json","usercache.json"]:safe(rel).write_text("[]\n")
    for rel in ["config/lunaeternal/world-edit.properties","config/lunaeternal/staff-roles.properties"]:safe(rel).write_text("# Preopening reset: no player assignments.\n")
    safe("config/ClaimBlocks/claims.json").write_text("[]\n")
    # Preserve scoreboard definitions/fake holders, remove former player scores/memberships.
    scoreboard=safe("world/data/scoreboard.dat");n=nbtlib.load(scoreboard);d=n["data"]
    old_names=names|ids
    scores=d.get("PlayerScores",nbtlib.List[nbtlib.Compound]());d["PlayerScores"]=nbtlib.List[nbtlib.Compound]([x for x in scores if str(x.get("Name","")) not in old_names])
    for team in d.get("Teams",[]):
        if "Players" in team:team["Players"]=nbtlib.List[nbtlib.String]([x for x in team["Players"] if str(x) not in old_names])
    n.save(scoreboard)
    for rel in ["world/level.dat","world/level.dat_old"]:
        p=safe(rel)
        if p.exists():
            n=nbtlib.load(p)
            if "Player" in n.get("Data",{}):del n["Data"]["Player"];n.save(p)
    # Only remove entities explicitly owned by former players. NPC/decorative entities are preserved.
    entity_before=collections.Counter();entity_after=collections.Counter();removed=collections.Counter();changed_regions=[]
    for p in safe("world").rglob("*.mca"):
        if p.parent.name!="entities":continue
        raw=bytearray(p.read_bytes());changed=False
        if not raw:continue  # Minecraft may create an empty region before its first entity save.
        assert len(raw)>=8192, "Truncated entity region header"
        for index in range(1024):
            entry=index*4;sector=int.from_bytes(raw[entry:entry+3],"big");capacity=raw[entry+3]
            if not sector:continue
            offset=sector*4096;length=int.from_bytes(raw[offset:offset+4],"big");compression=raw[offset+4]
            assert compression in (1,2,3),"Unsupported external/compressed entity chunk"
            content=bytes(raw[offset+5:offset+4+length]);content=zlib.decompress(content) if compression==2 else gzip.decompress(content) if compression==1 else content
            chunk=nbtlib.File.parse(io.BytesIO(content));entities=chunk.get("Entities",[]);kept=[]
            for entity in entities:
                kind=str(entity.get("id",""));entity_before[kind]+=1
                if owned(entity):removed[kind]+=1
                else:kept.append(entity);entity_after[kind]+=1
            if len(kept)==len(entities):continue
            chunk["Entities"]=nbtlib.List[nbtlib.Compound](kept);buffer=io.BytesIO();chunk.write(buffer);payload=buffer.getvalue()
            payload=zlib.compress(payload) if compression==2 else gzip.compress(payload) if compression==1 else payload
            encoded=(len(payload)+1).to_bytes(4,"big")+bytes([compression])+payload;needed=(len(encoded)+4095)//4096
            if needed>capacity:
                sector=(len(raw)+4095)//4096;offset=sector*4096;capacity=needed;raw.extend(b"\0"*(offset-len(raw)+capacity*4096))
                raw[entry:entry+4]=sector.to_bytes(3,"big")+bytes([capacity])
            raw[offset:offset+capacity*4096]=encoded+b"\0"*(capacity*4096-len(encoded));raw[4096+entry:4100+entry]=int(time.time()).to_bytes(4,"big");changed=True
        if changed:
            p.write_bytes(raw);changed_regions.append(p.relative_to(ROOT).as_posix())
    for kind,count in entity_before.items():
        if kind not in removed:assert entity_after[kind]==count
    # One transaction, explicit server policy/catalog allowlist. No schema destruction.
    delete=sorted(tables-PRESERVE)
    statements=["SET FOREIGN_KEY_CHECKS=0","START TRANSACTION"]+["DELETE FROM `"+t+"`" for t in delete]
    statements += ["UPDATE santuario SET owner_id=NULL,permanente=0,expira_ms=NULL,foto_id=NULL,titulo='',descripcion='',honores=0,tocado_ms=0","COMMIT","SET FOREIGN_KEY_CHECKS=1"]
    sql(";".join(statements))
    for t in delete:assert int(sql("SELECT COUNT(*) FROM `"+t+"`"))==0,t
    # IDs start clean; all referencing player tables have already been emptied.
    auto=sql("SELECT TABLE_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='"+DB+"' AND EXTRA LIKE '%auto_increment%'").splitlines()
    for t in auto:
        if t in delete:sql("ALTER TABLE `"+t+"` AUTO_INCREMENT=1")
    assert sql("SELECT COUNT(*) FROM santuario WHERE owner_id IS NOT NULL OR foto_id IS NOT NULL OR honores<>0")=="0"
    # Verify terrain/POI/datapacks have not changed before applying the known dimension-dispatch correction.
    protected=json.loads((backup/"protected-world-hashes.json").read_text())
    assert all(hashlib.sha256(safe(rel).read_bytes()).hexdigest()==h for rel,h in protected)
    corrected=pathlib.Path("/tmp/pokereport-angry-alphas-worlds-corrected.zip")
    shutil.copy2(corrected,safe("world/datapacks/pokereport-angry-alphas-worlds.zip"))
    # Temporarily block connections for empty-state smoke tests, then restore previous policy.
    properties=safe("server.properties");original=properties.read_text();whitelist=next((x.split("=",1)[1] for x in original.splitlines() if x.startswith("white-list=")),"false")
    lines=["white-list=true" if x.startswith("white-list=") else x for x in original.splitlines()];properties.write_text("\n".join(lines)+"\n")
    subprocess.run(["chown","-R","pterodactyl:pterodactyl",str(safe("EasyAuth"))],check=True)
    for rel in ["ops.json","whitelist.json","banned-players.json","banned-ips.json","usercache.json","server.properties","world/data/scoreboard.dat","world/datapacks/pokereport-angry-alphas-worlds.zip","config/lunaeternal/world-edit.properties","config/lunaeternal/staff-roles.properties","config/ClaimBlocks/claims.json"]:
        subprocess.run(["chown","pterodactyl:pterodactyl",str(safe(rel))],check=True)
    report={"backup":str(backup),"original_whitelist":whitelist,"players_before":before["player"],"players_after":0,"auth_accounts_after":0,"tebex_payments_before":before["tebex_payment"],"tebex_payments_after":0,"cleared_tables":delete,"preserved_catalog_tables":sorted(PRESERVE),"moved_paths":moved,"world_protected_files_verified":len(protected),"owned_entities_removed":dict(removed),"changed_entity_regions":changed_regions,"entity_counts_before":dict(entity_before),"entity_counts_after":dict(entity_after),"staging_stopped":True,"remote_tebex_queue_commands":0,"remote_tebex_payment_history_deleted":False,"state":"WIPED_OFFLINE"}
    (backup/"reset-report.json").write_text(json.dumps(report,indent=2));pathlib.Path("/tmp/player-reset-report.json").write_text(json.dumps(report,indent=2))
    print(json.dumps({k:report[k] for k in ["backup","players_before","players_after","auth_accounts_after","tebex_payments_before","tebex_payments_after","world_protected_files_verified","owned_entities_removed","state"]}),flush=True)
if __name__=="__main__":main()
