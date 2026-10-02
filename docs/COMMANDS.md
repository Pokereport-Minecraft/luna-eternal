# Comandos — inventario real prelaunch

Exportación Brigadier de consola en staging con los mods reales: 2247 usos. Después de desactivar el conector externo Tebex: 2236. Un uso es una ruta/sintaxis, no un comando independiente. `getAllUsage` enumera ramas y no demuestra que todas se hayan ejecutado ni que sus argumentos sean válidos.

| Familia | Finalidad / autoridad comprobada | Validación |
|---|---|---|
| pradmin editmode on/off | OP4 + edit/editmode; Creative y sesión | Política y edición sintética PASS |
| pradmin status | Superior autorizado o consola | Lectura |
| pradmin commands | Solo consola; exporta logs/pokereport-commands.txt | PASS |
| pradmin selftest | Solo consola + schema exacto de staging | 21 PASS; prohibido en producción |
| luna autotest | Suite que escribe fixtures | 753 PASS solo staging |
| luna dar/rango/testrango/constructor | Alto staff + commands adicional | Revisión de gate; no ejecución destructiva |
| luna gimnasio/decorar/naranja | Alto staff + commands; editmode en safe | Revisión de gate; no ejecución destructiva |
| Vanilla crítico (op/give/fill/execute/function/etc.) | OP4 + commands; terreno safe además editmode | Pruebas de política; parte de servidor |
| WorldEdit // y aliases | OP4 + edit/editmode + Creative + sesión | Gate revisado; no prueba de cada brush |
| Axiom payload | Edición autorizada en safe | Gate revisado; cliente pendiente |
| Tebex | Conector real; desactivado en copia QA | Compra/replay externos pendientes |
| Comandos jugador/Cobblemon/economía/teleports | Requisitos de cada registrador existente | Inventariados; no todos ejecutados |
| Otros comandos de terceros | Requisitos propios del mod | Revisión granular pendiente |

La nueva barrera no concede acceso: se añade a los requisitos originales. Consola conserva autoridad nativa. No atribuir permisos de LuckPerms: no está instalado. La lista CRITICAL/TERRAIN/WORLDEDIT exacta está en CommandSafetyPolicy.java; aliases nuevos requieren revisión. Namespaces se normalizan; execute/function/schedule/clone necesitan sesión en cualquier mundo para cerrar destinos seguros indirectos; gimnasio/decorar/naranja también. Comandos de terceros fuera de esas listas NO quedan certificados como seguros.

Sintaxis nueva: `/pradmin editmode on`, `/pradmin editmode off`, `/pradmin status`, `/pradmin commands`, `/pradmin selftest`. Los permisos están en ADMIN_ROLES.md. El archivo completo de evidencia con Tebex está en prelaunch/runtime-commands-with-tebex.txt.

## Todas las raíces y sintaxis descubiertas

Cada raíz de abajo está realmente registrada. Estado predeterminado: INVENTARIADO, NO EJECUTADO, permisos del registrador original. Las excepciones verificadas se detallan arriba. Se conserva la forma exportada de WorldEdit (doble slash incluido).

### /!

```text
/!
/! <args>
```

### /,

```text
/,
/, <args>
```

### /.s

```text
/.s
/.s <args>
```

### //

```text
//
// <args>
```

### //br

```text
//br
//br <args>
```

### //brush

```text
//brush
//brush <args>
```

### //calc

```text
//calc
//calc <args>
```

### //calculate

```text
//calculate
//calculate <args>
```

### //center

```text
//center
//center <args>
```

### //chunk

```text
//chunk
//chunk <args>
```

### //clearhistory

```text
//clearhistory
//clearhistory <args>
```

### //cone

```text
//cone
//cone <args>
```

### //contract

```text
//contract
//contract <args>
```

### //copy

```text
//copy
//copy <args>
```

### //count

```text
//count
//count <args>
```

### //curve

```text
//curve
//curve <args>
```

### //cut

```text
//cut
//cut <args>
```

### //cyl

```text
//cyl
//cyl <args>
```

### //deform

```text
//deform
//deform <args>
```

### //desel

```text
//desel
//desel <args>
```

### //deselect

```text
//deselect
//deselect <args>
```

### //distr

```text
//distr
//distr <args>
```

### //drain

```text
//drain
//drain <args>
```

### //drawsel

```text
//drawsel
//drawsel <args>
```

### //eval

```text
//eval
//eval <args>
```

### //evaluate

```text
//evaluate
//evaluate <args>
```

### //ex

```text
//ex
//ex <args>
```

### //expand

```text
//expand
//expand <args>
```

### //ext

```text
//ext
//ext <args>
```

### //extinguish

```text
//extinguish
//extinguish <args>
```

### //faces

```text
//faces
//faces <args>
```

### //fast

```text
//fast
//fast <args>
```

### //feature

```text
//feature
//feature <args>
```

### //fill

```text
//fill
//fill <args>
```

### //fillr

```text
//fillr
//fillr <args>
```

### //fixlava

```text
//fixlava
//fixlava <args>
```

### //fixwater

```text
//fixwater
//fixwater <args>
```

### //flip

```text
//flip
//flip <args>
```

### //flora

```text
//flora
//flora <args>
```

### //forest

```text
//forest
//forest <args>
```

### //g

```text
//g
//g <args>
```

### //gb

```text
//gb
//gb <args>
```

### //gen

```text
//gen
//gen <args>
```

### //genbiome

```text
//genbiome
//genbiome <args>
```

### //generate

```text
//generate
//generate <args>
```

### //generatebiome

```text
//generatebiome
//generatebiome <args>
```

### //gmask

```text
//gmask
//gmask <args>
```

### //green

```text
//green
//green <args>
```

### //hcyl

```text
//hcyl
//hcyl <args>
```

### //help

```text
//help
//help <args>
```

### //hollow

```text
//hollow
//hollow <args>
```

### //hpos1

```text
//hpos1
//hpos1 <args>
```

### //hpos2

```text
//hpos2
//hpos2 <args>
```

### //hpyramid

```text
//hpyramid
//hpyramid <args>
```

### //hsphere

```text
//hsphere
//hsphere <args>
```

### //inset

```text
//inset
//inset <args>
```

### //l

```text
//l
//l <args>
```

### //limit

```text
//limit
//limit <args>
```

### //line

```text
//line
//line <args>
```

### //lrbuild

```text
//lrbuild
//lrbuild <args>
```

### //material

```text
//material
//material <args>
```

### //middle

```text
//middle
//middle <args>
```

### //move

```text
//move
//move <args>
```

### //naturalize

```text
//naturalize
//naturalize <args>
```

### //navwand

```text
//navwand
//navwand <args>
```

### //outline

```text
//outline
//outline <args>
```

### //outset

```text
//outset
//outset <args>
```

### //overlay

```text
//overlay
//overlay <args>
```

### //paste

```text
//paste
//paste <args>
```

### //perf

```text
//perf
//perf <args>
```

### //placement

```text
//placement
//placement <args>
```

### //pos

```text
//pos
//pos <args>
```

### //pos1

```text
//pos1
//pos1 <args>
```

### //pos2

```text
//pos2
//pos2 <args>
```

### //pyramid

```text
//pyramid
//pyramid <args>
```

### //re

```text
//re
//re <args>
```

### //redo

```text
//redo
//redo <args>
```

### //regen

```text
//regen
//regen <args>
```

### //removeabove

```text
//removeabove
//removeabove <args>
```

### //removebelow

```text
//removebelow
//removebelow <args>
```

### //removenear

```text
//removenear
//removenear <args>
```

### //reorder

```text
//reorder
//reorder <args>
```

### //rep

```text
//rep
//rep <args>
```

### //replace

```text
//replace
//replace <args>
```

### //replacenear

```text
//replacenear
//replacenear <args>
```

### //restore

```text
//restore
//restore <args>
```

### //rotate

```text
//rotate
//rotate <args>
```

### //schem

```text
//schem
//schem <args>
```

### //schematic

```text
//schematic
//schematic <args>
```

### //search

```text
//search
//search <args>
```

### //searchitem

```text
//searchitem
//searchitem <args>
```

### //sel

```text
//sel
//sel <args>
```

### //selwand

```text
//selwand
//selwand <args>
```

### //set

```text
//set
//set <args>
```

### //setbiome

```text
//setbiome
//setbiome <args>
```

### //shift

```text
//shift
//shift <args>
```

### //size

```text
//size
//size <args>
```

### //smooth

```text
//smooth
//smooth <args>
```

### //snow

```text
//snow
//snow <args>
```

### //snowsmooth

```text
//snowsmooth
//snowsmooth <args>
```

### //solve

```text
//solve
//solve <args>
```

### //sphere

```text
//sphere
//sphere <args>
```

### //stack

```text
//stack
//stack <args>
```

### //structure

```text
//structure
//structure <args>
```

### //thaw

```text
//thaw
//thaw <args>
```

### //timeout

```text
//timeout
//timeout <args>
```

### //toggleplace

```text
//toggleplace
//toggleplace <args>
```

### //trim

```text
//trim
//trim <args>
```

### //undo

```text
//undo
//undo <args>
```

### //update

```text
//update
//update <args>
```

### //walls

```text
//walls
//walls <args>
```

### //wand

```text
//wand
//wand <args>
```

### //watchdog

```text
//watchdog
//watchdog <args>
```

### //world

```text
//world
//world <args>
```

### /;

```text
/;
/; <args>
```

### /abandonmultibattleteam

```text
/abandonmultibattleteam
```

### /abandonmultiteam

```text
/abandonmultiteam
```

### /accessories

```text
/accessories attribute modifier add <attribute> <id> <value> add_multiplied_base <slot> <isStackable>
/accessories attribute modifier add <attribute> <id> <value> add_multiplied_total <slot> <isStackable>
/accessories attribute modifier add <attribute> <id> <value> add_value <slot> <isStackable>
/accessories attribute modifier get <attribute> <id>
/accessories attribute modifier get <attribute> <id> <scale>
/accessories attribute modifier remove <attribute> <id>
/accessories edit
/accessories edit <entity>
/accessories log entity_bindings
/accessories log groups
/accessories log slots
/accessories nest <item>
/accessories slot add invalid <slot>
/accessories slot add valid <slot>
/accessories slot remove invalid <slot>
/accessories slot remove valid <slot>
/accessories stack-sizing <value>
/accessories stack-sizing useStackSize <value>
```

### /account

```text
/account changePassword <old password>
/account changePassword <old password> <new password>
/account online <password>
/account online <password> <confirm>
/account unregister
/account unregister <password>
```

### /advancement

```text
/advancement grant <targets> everything
/advancement grant <targets> from <advancement>
/advancement grant <targets> only <advancement>
/advancement grant <targets> only <advancement> <criterion>
/advancement grant <targets> through <advancement>
/advancement grant <targets> until <advancement>
/advancement revoke <targets> everything
/advancement revoke <targets> from <advancement>
/advancement revoke <targets> only <advancement>
/advancement revoke <targets> only <advancement> <criterion>
/advancement revoke <targets> through <advancement>
/advancement revoke <targets> until <advancement>
```

### /almanac

```text
/almanac reload
/almanac reload <config>
```

### /anvil

```text
/anvil
```

### /applyplayertexture

```text
/applyplayertexture <player>
```

### /asc

```text
/asc
/asc <args>
```

### /ascend

```text
/ascend
/ascend <args>
```

### /attribute

```text
/attribute <target> <attribute> base get
/attribute <target> <attribute> base get <scale>
/attribute <target> <attribute> base set <value>
/attribute <target> <attribute> get
/attribute <target> <attribute> get <scale>
/attribute <target> <attribute> modifier add <id> <value> add_multiplied_base
/attribute <target> <attribute> modifier add <id> <value> add_multiplied_total
/attribute <target> <attribute> modifier add <id> <value> add_value
/attribute <target> <attribute> modifier remove <id>
/attribute <target> <attribute> modifier value get <id>
/attribute <target> <attribute> modifier value get <id> <scale>
```

### /auth

```text
/auth clearUuid <username>
/auth getOnlinePlayers
/auth getPlayerInfo <username>
/auth getUuid <username>
/auth list
/auth markAsOffline <username>
/auth markAsOnline <username>
/auth register <username> <password>
/auth reload
/auth remove <username>
/auth setGlobalPassword <password>
/auth setGlobalPassword <password> <singleUse>
/auth setSpawn
/auth setSpawn <dimension> <position>
/auth setSpawn <dimension> <position> <angle>
/auth setUuid <username> <uuid>
/auth update <username> <password>
```

### /back

```text
/back
```

### /ban

```text
/ban <targets>
/ban <targets> <reason>
```

### /ban-ip

```text
/ban-ip <target>
/ban-ip <target> <reason>
```

### /banlist

```text
/banlist
/banlist ips
/banlist players
```

### /basura

```text
/basura
```

### /bedrockparticle

```text
/bedrockparticle <effect> <target>
/bedrockparticle <effect> <target> <locator>
/bedrockparticle <effect> <world> <pos>
```

### /behaviouredit

```text
/behaviouredit
```

### /biomeinfo

```text
/biomeinfo
/biomeinfo <args>
```

### /biomelist

```text
/biomelist
/biomelist <args>
```

### /biomels

```text
/biomels
/biomels <args>
```

### /bossbar

```text
/bossbar add <id> <name>
/bossbar get <id> max
/bossbar get <id> players
/bossbar get <id> value
/bossbar get <id> visible
/bossbar list
/bossbar remove <id>
/bossbar set <id> color blue
/bossbar set <id> color green
/bossbar set <id> color pink
/bossbar set <id> color purple
/bossbar set <id> color red
/bossbar set <id> color white
/bossbar set <id> color yellow
/bossbar set <id> max <max>
/bossbar set <id> name <name>
/bossbar set <id> players
/bossbar set <id> players <targets>
/bossbar set <id> style notched_10
/bossbar set <id> style notched_12
/bossbar set <id> style notched_20
/bossbar set <id> style notched_6
/bossbar set <id> style progress
/bossbar set <id> value <value>
/bossbar set <id> visible <visible>
```

### /boxcount

```text
/boxcount add <player> <amount>
/boxcount query <player>
/boxcount remove <player> <amount>
/boxcount set <player> <amount>
```

### /br

```text
/br
/br <args>
```

### /brush

```text
/brush
/brush <args>
```

### /butcher

```text
/butcher
/butcher <args>
```

### /buy

```text
/buy
```

### /cb

```text
/cb
/cb add <target>
/cb delete
/cb get <type>
/cb give <user> <type> <amount>
/cb info
/cb menu
/cb menu <target>
/cb view
```

### /ceil

```text
/ceil
/ceil <args>
```

### /changeeyeheight

```text
/changeeyeheight <pokemon> flying <height>
/changeeyeheight <pokemon> standing <height>
/changeeyeheight <pokemon> swimming <height>
```

### /changescaleandsize

```text
/changescaleandsize <pokemon>
/changescaleandsize <pokemon> <scale> <width> <height>
```

### /changewalkspeed

```text
/changewalkspeed
/changewalkspeed <pokemon> <walkSpeed>
```

### /changewallpaper

```text
/changewallpaper <player> <box> <wallpaper>
```

### /checkspawn

```text
/checkspawn <bucket>
```

### /checkspawns

```text
/checkspawns
```

### /chunkinfo

```text
/chunkinfo
/chunkinfo <args>
```

### /chunky

```text
/chunky
/chunky border
/chunky border add
/chunky border add <world>
/chunky border add <world> <shape>
/chunky border add <world> <shape> <centerX>
/chunky border add <world> <shape> <centerX> <centerZ>
/chunky border add <world> <shape> <centerX> <centerZ> <radiusX>
/chunky border add <world> <shape> <centerX> <centerZ> <radiusX> <radiusZ>
/chunky border bypass
/chunky border bypass <player>
/chunky border help
/chunky border list
/chunky border load
/chunky border load <world>
/chunky border remove
/chunky border remove <world>
/chunky border wrap
/chunky border wrap <wrap>
/chunky cancel
/chunky cancel <world>
/chunky center
/chunky center <x>
/chunky center <x> <z>
/chunky confirm
/chunky continue
/chunky continue <world>
/chunky corners
/chunky corners <x1>
/chunky corners <x1> <z1>
/chunky corners <x1> <z1> <x2>
/chunky corners <x1> <z1> <x2> <z2>
/chunky help
/chunky help <page>
/chunky pattern
/chunky pattern <pattern>
/chunky pattern <pattern> <value>
/chunky pause
/chunky pause <world>
/chunky progress
/chunky quiet
/chunky quiet <interval>
/chunky radius
/chunky radius <radius>
/chunky radius <radius> <radius>
/chunky reload
/chunky reload <type>
/chunky selection
/chunky shape
/chunky shape <shape>
/chunky silent
/chunky spawn
/chunky start
/chunky start <world>
/chunky start <world> <shape>
/chunky start <world> <shape> <centerX>
/chunky start <world> <shape> <centerX> <centerZ>
/chunky start <world> <shape> <centerX> <centerZ> <radiusX>
/chunky start <world> <shape> <centerX> <centerZ> <radiusX> <radiusZ>
/chunky trim
/chunky trim <world>
/chunky trim <world> <shape>
/chunky trim <world> <shape> <centerX>
/chunky trim <world> <shape> <centerX> <centerZ>
/chunky trim <world> <shape> <centerX> <centerZ> <radiusX>
/chunky trim <world> <shape> <centerX> <centerZ> <radiusX> <radiusZ>
/chunky trim <world> <shape> <centerX> <centerZ> <radiusX> <radiusZ> <trimMode>
/chunky trim <world> <shape> <centerX> <centerZ> <radiusX> <radiusZ> <trimMode> <inhabited>
/chunky world
/chunky world <world>
/chunky worldborder
```

### /clan

```text
/clan delhome
/clan home
/clan sethome
```

### /clear

```text
/clear
/clear <targets>
/clear <targets> <item>
/clear <targets> <item> <maxCount>
```

### /clearclipboard

```text
/clearclipboard
/clearclipboard <args>
```

### /clearhistory

```text
/clearhistory
/clearhistory <args>
```

### /clearparty

```text
/clearparty <player>
```

### /clearpc

```text
/clearpc <player>
```

### /clone

```text
/clone <begin> <end> <destination>
/clone <begin> <end> <destination> filtered <filter>
/clone <begin> <end> <destination> filtered <filter> force
/clone <begin> <end> <destination> filtered <filter> move
/clone <begin> <end> <destination> filtered <filter> normal
/clone <begin> <end> <destination> masked
/clone <begin> <end> <destination> masked force
/clone <begin> <end> <destination> masked move
/clone <begin> <end> <destination> masked normal
/clone <begin> <end> <destination> replace
/clone <begin> <end> <destination> replace force
/clone <begin> <end> <destination> replace move
/clone <begin> <end> <destination> replace normal
/clone <begin> <end> to <targetDimension> <destination>
/clone <begin> <end> to <targetDimension> <destination> filtered <filter>
/clone <begin> <end> to <targetDimension> <destination> filtered <filter> force
/clone <begin> <end> to <targetDimension> <destination> filtered <filter> move
/clone <begin> <end> to <targetDimension> <destination> filtered <filter> normal
/clone <begin> <end> to <targetDimension> <destination> masked
/clone <begin> <end> to <targetDimension> <destination> masked force
/clone <begin> <end> to <targetDimension> <destination> masked move
/clone <begin> <end> to <targetDimension> <destination> masked normal
/clone <begin> <end> to <targetDimension> <destination> replace
/clone <begin> <end> to <targetDimension> <destination> replace force
/clone <begin> <end> to <targetDimension> <destination> replace move
/clone <begin> <end> to <targetDimension> <destination> replace normal
/clone from <sourceDimension> <begin> <end> <destination>
/clone from <sourceDimension> <begin> <end> <destination> filtered <filter>
/clone from <sourceDimension> <begin> <end> <destination> filtered <filter> force
/clone from <sourceDimension> <begin> <end> <destination> filtered <filter> move
/clone from <sourceDimension> <begin> <end> <destination> filtered <filter> normal
/clone from <sourceDimension> <begin> <end> <destination> masked
/clone from <sourceDimension> <begin> <end> <destination> masked force
/clone from <sourceDimension> <begin> <end> <destination> masked move
/clone from <sourceDimension> <begin> <end> <destination> masked normal
/clone from <sourceDimension> <begin> <end> <destination> replace
/clone from <sourceDimension> <begin> <end> <destination> replace force
/clone from <sourceDimension> <begin> <end> <destination> replace move
/clone from <sourceDimension> <begin> <end> <destination> replace normal
/clone from <sourceDimension> <begin> <end> to <targetDimension> <destination>
/clone from <sourceDimension> <begin> <end> to <targetDimension> <destination> filtered <filter>
/clone from <sourceDimension> <begin> <end> to <targetDimension> <destination> filtered <filter> force
/clone from <sourceDimension> <begin> <end> to <targetDimension> <destination> filtered <filter> move
/clone from <sourceDimension> <begin> <end> to <targetDimension> <destination> filtered <filter> normal
/clone from <sourceDimension> <begin> <end> to <targetDimension> <destination> masked
/clone from <sourceDimension> <begin> <end> to <targetDimension> <destination> masked force
/clone from <sourceDimension> <begin> <end> to <targetDimension> <destination> masked move
/clone from <sourceDimension> <begin> <end> to <targetDimension> <destination> masked normal
/clone from <sourceDimension> <begin> <end> to <targetDimension> <destination> replace
/clone from <sourceDimension> <begin> <end> to <targetDimension> <destination> replace force
/clone from <sourceDimension> <begin> <end> to <targetDimension> <destination> replace move
/clone from <sourceDimension> <begin> <end> to <targetDimension> <destination> replace normal
```

### /cobblecard

```text
/cobblecard carddex fill
/cobblecard custombooster
/cobblecard fill_cabinet
/cobblecard give <target> <pokemon> <shiny> <rarity>
/cobblecard give <target> <pokemon> <shiny> <rarity> <stat> <stat_value>
/cobblecard give <target> <pokemon> <shiny> <rarity> <stat> <stat_value> <background>
/cobblecard give <target> <pokemon> <shiny> <rarity> <stat> <stat_value> <background> <effect>
/cobblecard render <pokemon> <shiny> <rarity>
/cobblecard render <pokemon> <shiny> <rarity> <background>
/cobblecard render <pokemon> <shiny> <rarity> <background> <effect>
/cobblecard render <pokemon> <shiny> <rarity> <background> <effect> more <pokemon2> <shiny2> <rarity2>
/cobblecard render <pokemon> <shiny> <rarity> <background> <effect> more <pokemon2> <shiny2> <rarity2> <background2>
/cobblecard render <pokemon> <shiny> <rarity> <background> <effect> more <pokemon2> <shiny2> <rarity2> <background2> <effect2>
/cobblecard render <pokemon> <shiny> <rarity> <background> <effect> more <pokemon2> <shiny2> <rarity2> <background2> <effect2> more <pokemon3> <shiny3> <rarity3>
/cobblecard render <pokemon> <shiny> <rarity> <background> <effect> more <pokemon2> <shiny2> <rarity2> <background2> <effect2> more <pokemon3> <shiny3> <rarity3> <background3>
/cobblecard render <pokemon> <shiny> <rarity> <background> <effect> more <pokemon2> <shiny2> <rarity2> <background2> <effect2> more <pokemon3> <shiny3> <rarity3> <background3> <effect3>
/cobblecard render <pokemon> <shiny> <rarity> <background> <effect> more <pokemon2> <shiny2> <rarity2> <background2> <effect2> more <pokemon3> <shiny3> <rarity3> <background3> <effect3> more <pokemon4> <shiny4> <rarity4>
/cobblecard render <pokemon> <shiny> <rarity> <background> <effect> more <pokemon2> <shiny2> <rarity2> <background2> <effect2> more <pokemon3> <shiny3> <rarity3> <background3> <effect3> more <pokemon4> <shiny4> <rarity4> <background4>
/cobblecard render <pokemon> <shiny> <rarity> <background> <effect> more <pokemon2> <shiny2> <rarity2> <background2> <effect2> more <pokemon3> <shiny3> <rarity3> <background3> <effect3> more <pokemon4> <shiny4> <rarity4> <background4> <effect4>
/cobblecard render <pokemon> <shiny> <rarity> <background> <effect> more <pokemon2> <shiny2> <rarity2> <background2> <effect2> more <pokemon3> <shiny3> <rarity3> <background3> <effect3> more <pokemon4> <shiny4> <rarity4> <background4> <effect4> more <pokemon5> <shiny5> <rarity5>
/cobblecard render <pokemon> <shiny> <rarity> <background> <effect> more <pokemon2> <shiny2> <rarity2> <background2> <effect2> more <pokemon3> <shiny3> <rarity3> <background3> <effect3> more <pokemon4> <shiny4> <rarity4> <background4> <effect4> more <pokemon5> <shiny5> <rarity5> <background5>
/cobblecard render <pokemon> <shiny> <rarity> <background> <effect> more <pokemon2> <shiny2> <rarity2> <background2> <effect2> more <pokemon3> <shiny3> <rarity3> <background3> <effect3> more <pokemon4> <shiny4> <rarity4> <background4> <effect4> more <pokemon5> <shiny5> <rarity5> <background5> <effect5>
/cobblecard workshop
```

### /cobblemon

```text
/cobblemon info
```

### /cobblemonclicktext

```text
/cobblemonclicktext <callback>
```

### /cobblemonconfig

```text
/cobblemonconfig
/cobblemonconfig reload
```

### /cobbreeding

```text
/cobbreeding egg decrypt <player>
/cobbreeding egg give <player> <properties>
/cobbreeding egg hatch <player>
/cobbreeding info
/cobbreeding reload
```

### /craft

```text
/craft
```

### /crd

```text
/crd cycleraids
/crd dens <position>
/crd dens <position> <dimension>
/crd dens <position> <dimension> boss <boss>
/crd dens <position> <dimension> boss <boss> <cycle_mode>
/crd dens <position> <dimension> boss <boss> <cycle_mode> <can_reset>
/crd dens <position> <dimension> bucket <bucket>
/crd dens <position> <dimension> bucket <bucket> <cycle_mode>
/crd dens <position> <dimension> bucket <bucket> <cycle_mode> <can_reset>
/crd dens <position> <dimension> random
/crd dens <position> <dimension> random <cycle_mode>
/crd dens <position> <dimension> random <cycle_mode> <can_reset>
/crd dens <position> <dimension> tier <tier>
/crd dens <position> <dimension> tier <tier> <cycle_mode>
/crd dens <position> <dimension> tier <tier> <cycle_mode> <can_reset>
/crd dens <position> boss <boss>
/crd dens <position> boss <boss> <cycle_mode>
/crd dens <position> boss <boss> <cycle_mode> <can_reset>
/crd dens <position> bucket <bucket>
/crd dens <position> bucket <bucket> <cycle_mode>
/crd dens <position> bucket <bucket> <cycle_mode> <can_reset>
/crd dens <position> random
/crd dens <position> random <cycle_mode>
/crd dens <position> random <cycle_mode> <can_reset>
/crd dens <position> tier <tier>
/crd dens <position> tier <tier> <cycle_mode>
/crd dens <position> tier <tier> <cycle_mode> <can_reset>
/crd forceclear
/crd forceclear <player>
/crd refresh
/crd refresh <player>
/crd resetclears <player>
/crd resetclears <pos>
/crd resetclears <pos> <dimension>
/crd resetclears <pos> <dimension> <player>
/crd spawnboss <pos>
/crd spawnboss <pos> <dimension>
/crd spawnboss <pos> <dimension> boss <boss>
/crd spawnboss <pos> <dimension> boss <boss> <noAI>
/crd spawnboss <pos> <dimension> boss <boss> <noAI> <isInvulnerable>
/crd spawnboss <pos> <dimension> boss <boss> <noAI> <isInvulnerable> <isPersistent>
/crd spawnboss <pos> <dimension> bucket <bucket>
/crd spawnboss <pos> <dimension> bucket <bucket> <noAI>
/crd spawnboss <pos> <dimension> bucket <bucket> <noAI> <isInvulnerable>
/crd spawnboss <pos> <dimension> bucket <bucket> <noAI> <isInvulnerable> <isPersistent>
/crd spawnboss <pos> <dimension> random
/crd spawnboss <pos> <dimension> random <noAI>
/crd spawnboss <pos> <dimension> random <noAI> <isInvulnerable>
/crd spawnboss <pos> <dimension> random <noAI> <isInvulnerable> <isPersistent>
/crd spawnboss <pos> <dimension> tier <tier>
/crd spawnboss <pos> <dimension> tier <tier> <noAI>
/crd spawnboss <pos> <dimension> tier <tier> <noAI> <isInvulnerable>
/crd spawnboss <pos> <dimension> tier <tier> <noAI> <isInvulnerable> <isPersistent>
/crd spawnboss <pos> boss <boss>
/crd spawnboss <pos> boss <boss> <noAI>
/crd spawnboss <pos> boss <boss> <noAI> <isInvulnerable>
/crd spawnboss <pos> boss <boss> <noAI> <isInvulnerable> <isPersistent>
/crd spawnboss <pos> bucket <bucket>
/crd spawnboss <pos> bucket <bucket> <noAI>
/crd spawnboss <pos> bucket <bucket> <noAI> <isInvulnerable>
/crd spawnboss <pos> bucket <bucket> <noAI> <isInvulnerable> <isPersistent>
/crd spawnboss <pos> random
/crd spawnboss <pos> random <noAI>
/crd spawnboss <pos> random <noAI> <isInvulnerable>
/crd spawnboss <pos> random <noAI> <isInvulnerable> <isPersistent>
/crd spawnboss <pos> tier <tier>
/crd spawnboss <pos> tier <tier> <noAI>
/crd spawnboss <pos> tier <tier> <noAI> <isInvulnerable>
/crd spawnboss <pos> tier <tier> <noAI> <isInvulnerable> <isPersistent>
```

### /cs

```text
/cs
/cs <args>
```

### /cycler

```text
/cycler
/cycler <args>
```

### /damage

```text
/damage <target> <amount>
/damage <target> <amount> <damageType>
/damage <target> <amount> <damageType> at <location>
/damage <target> <amount> <damageType> by <entity>
/damage <target> <amount> <damageType> by <entity> from <cause>
```

### /data

```text
/data get block <targetPos>
/data get block <targetPos> <path>
/data get block <targetPos> <path> <scale>
/data get entity <target>
/data get entity <target> <path>
/data get entity <target> <path> <scale>
/data get storage <target>
/data get storage <target> <path>
/data get storage <target> <path> <scale>
/data merge block <targetPos> <nbt>
/data merge entity <target> <nbt>
/data merge storage <target> <nbt>
/data modify block <targetPos> <targetPath> append from block <sourcePos>
/data modify block <targetPos> <targetPath> append from block <sourcePos> <sourcePath>
/data modify block <targetPos> <targetPath> append from entity <source>
/data modify block <targetPos> <targetPath> append from entity <source> <sourcePath>
/data modify block <targetPos> <targetPath> append from storage <source>
/data modify block <targetPos> <targetPath> append from storage <source> <sourcePath>
/data modify block <targetPos> <targetPath> append string block <sourcePos>
/data modify block <targetPos> <targetPath> append string block <sourcePos> <sourcePath>
/data modify block <targetPos> <targetPath> append string block <sourcePos> <sourcePath> <start>
/data modify block <targetPos> <targetPath> append string block <sourcePos> <sourcePath> <start> <end>
/data modify block <targetPos> <targetPath> append string entity <source>
/data modify block <targetPos> <targetPath> append string entity <source> <sourcePath>
/data modify block <targetPos> <targetPath> append string entity <source> <sourcePath> <start>
/data modify block <targetPos> <targetPath> append string entity <source> <sourcePath> <start> <end>
/data modify block <targetPos> <targetPath> append string storage <source>
/data modify block <targetPos> <targetPath> append string storage <source> <sourcePath>
/data modify block <targetPos> <targetPath> append string storage <source> <sourcePath> <start>
/data modify block <targetPos> <targetPath> append string storage <source> <sourcePath> <start> <end>
/data modify block <targetPos> <targetPath> append value <value>
/data modify block <targetPos> <targetPath> insert <index> from block <sourcePos>
/data modify block <targetPos> <targetPath> insert <index> from block <sourcePos> <sourcePath>
/data modify block <targetPos> <targetPath> insert <index> from entity <source>
/data modify block <targetPos> <targetPath> insert <index> from entity <source> <sourcePath>
/data modify block <targetPos> <targetPath> insert <index> from storage <source>
/data modify block <targetPos> <targetPath> insert <index> from storage <source> <sourcePath>
/data modify block <targetPos> <targetPath> insert <index> string block <sourcePos>
/data modify block <targetPos> <targetPath> insert <index> string block <sourcePos> <sourcePath>
/data modify block <targetPos> <targetPath> insert <index> string block <sourcePos> <sourcePath> <start>
/data modify block <targetPos> <targetPath> insert <index> string block <sourcePos> <sourcePath> <start> <end>
/data modify block <targetPos> <targetPath> insert <index> string entity <source>
/data modify block <targetPos> <targetPath> insert <index> string entity <source> <sourcePath>
/data modify block <targetPos> <targetPath> insert <index> string entity <source> <sourcePath> <start>
/data modify block <targetPos> <targetPath> insert <index> string entity <source> <sourcePath> <start> <end>
/data modify block <targetPos> <targetPath> insert <index> string storage <source>
/data modify block <targetPos> <targetPath> insert <index> string storage <source> <sourcePath>
/data modify block <targetPos> <targetPath> insert <index> string storage <source> <sourcePath> <start>
/data modify block <targetPos> <targetPath> insert <index> string storage <source> <sourcePath> <start> <end>
/data modify block <targetPos> <targetPath> insert <index> value <value>
/data modify block <targetPos> <targetPath> merge from block <sourcePos>
/data modify block <targetPos> <targetPath> merge from block <sourcePos> <sourcePath>
/data modify block <targetPos> <targetPath> merge from entity <source>
/data modify block <targetPos> <targetPath> merge from entity <source> <sourcePath>
/data modify block <targetPos> <targetPath> merge from storage <source>
/data modify block <targetPos> <targetPath> merge from storage <source> <sourcePath>
/data modify block <targetPos> <targetPath> merge string block <sourcePos>
/data modify block <targetPos> <targetPath> merge string block <sourcePos> <sourcePath>
/data modify block <targetPos> <targetPath> merge string block <sourcePos> <sourcePath> <start>
/data modify block <targetPos> <targetPath> merge string block <sourcePos> <sourcePath> <start> <end>
/data modify block <targetPos> <targetPath> merge string entity <source>
/data modify block <targetPos> <targetPath> merge string entity <source> <sourcePath>
/data modify block <targetPos> <targetPath> merge string entity <source> <sourcePath> <start>
/data modify block <targetPos> <targetPath> merge string entity <source> <sourcePath> <start> <end>
/data modify block <targetPos> <targetPath> merge string storage <source>
/data modify block <targetPos> <targetPath> merge string storage <source> <sourcePath>
/data modify block <targetPos> <targetPath> merge string storage <source> <sourcePath> <start>
/data modify block <targetPos> <targetPath> merge string storage <source> <sourcePath> <start> <end>
/data modify block <targetPos> <targetPath> merge value <value>
/data modify block <targetPos> <targetPath> prepend from block <sourcePos>
/data modify block <targetPos> <targetPath> prepend from block <sourcePos> <sourcePath>
/data modify block <targetPos> <targetPath> prepend from entity <source>
/data modify block <targetPos> <targetPath> prepend from entity <source> <sourcePath>
/data modify block <targetPos> <targetPath> prepend from storage <source>
/data modify block <targetPos> <targetPath> prepend from storage <source> <sourcePath>
/data modify block <targetPos> <targetPath> prepend string block <sourcePos>
/data modify block <targetPos> <targetPath> prepend string block <sourcePos> <sourcePath>
/data modify block <targetPos> <targetPath> prepend string block <sourcePos> <sourcePath> <start>
/data modify block <targetPos> <targetPath> prepend string block <sourcePos> <sourcePath> <start> <end>
/data modify block <targetPos> <targetPath> prepend string entity <source>
/data modify block <targetPos> <targetPath> prepend string entity <source> <sourcePath>
/data modify block <targetPos> <targetPath> prepend string entity <source> <sourcePath> <start>
/data modify block <targetPos> <targetPath> prepend string entity <source> <sourcePath> <start> <end>
/data modify block <targetPos> <targetPath> prepend string storage <source>
/data modify block <targetPos> <targetPath> prepend string storage <source> <sourcePath>
/data modify block <targetPos> <targetPath> prepend string storage <source> <sourcePath> <start>
/data modify block <targetPos> <targetPath> prepend string storage <source> <sourcePath> <start> <end>
/data modify block <targetPos> <targetPath> prepend value <value>
/data modify block <targetPos> <targetPath> set from block <sourcePos>
/data modify block <targetPos> <targetPath> set from block <sourcePos> <sourcePath>
/data modify block <targetPos> <targetPath> set from entity <source>
/data modify block <targetPos> <targetPath> set from entity <source> <sourcePath>
/data modify block <targetPos> <targetPath> set from storage <source>
/data modify block <targetPos> <targetPath> set from storage <source> <sourcePath>
/data modify block <targetPos> <targetPath> set string block <sourcePos>
/data modify block <targetPos> <targetPath> set string block <sourcePos> <sourcePath>
/data modify block <targetPos> <targetPath> set string block <sourcePos> <sourcePath> <start>
/data modify block <targetPos> <targetPath> set string block <sourcePos> <sourcePath> <start> <end>
/data modify block <targetPos> <targetPath> set string entity <source>
/data modify block <targetPos> <targetPath> set string entity <source> <sourcePath>
/data modify block <targetPos> <targetPath> set string entity <source> <sourcePath> <start>
/data modify block <targetPos> <targetPath> set string entity <source> <sourcePath> <start> <end>
/data modify block <targetPos> <targetPath> set string storage <source>
/data modify block <targetPos> <targetPath> set string storage <source> <sourcePath>
/data modify block <targetPos> <targetPath> set string storage <source> <sourcePath> <start>
/data modify block <targetPos> <targetPath> set string storage <source> <sourcePath> <start> <end>
/data modify block <targetPos> <targetPath> set value <value>
/data modify entity <target> <targetPath> append from block <sourcePos>
/data modify entity <target> <targetPath> append from block <sourcePos> <sourcePath>
/data modify entity <target> <targetPath> append from entity <source>
/data modify entity <target> <targetPath> append from entity <source> <sourcePath>
/data modify entity <target> <targetPath> append from storage <source>
/data modify entity <target> <targetPath> append from storage <source> <sourcePath>
/data modify entity <target> <targetPath> append string block <sourcePos>
/data modify entity <target> <targetPath> append string block <sourcePos> <sourcePath>
/data modify entity <target> <targetPath> append string block <sourcePos> <sourcePath> <start>
/data modify entity <target> <targetPath> append string block <sourcePos> <sourcePath> <start> <end>
/data modify entity <target> <targetPath> append string entity <source>
/data modify entity <target> <targetPath> append string entity <source> <sourcePath>
/data modify entity <target> <targetPath> append string entity <source> <sourcePath> <start>
/data modify entity <target> <targetPath> append string entity <source> <sourcePath> <start> <end>
/data modify entity <target> <targetPath> append string storage <source>
/data modify entity <target> <targetPath> append string storage <source> <sourcePath>
/data modify entity <target> <targetPath> append string storage <source> <sourcePath> <start>
/data modify entity <target> <targetPath> append string storage <source> <sourcePath> <start> <end>
/data modify entity <target> <targetPath> append value <value>
/data modify entity <target> <targetPath> insert <index> from block <sourcePos>
/data modify entity <target> <targetPath> insert <index> from block <sourcePos> <sourcePath>
/data modify entity <target> <targetPath> insert <index> from entity <source>
/data modify entity <target> <targetPath> insert <index> from entity <source> <sourcePath>
/data modify entity <target> <targetPath> insert <index> from storage <source>
/data modify entity <target> <targetPath> insert <index> from storage <source> <sourcePath>
/data modify entity <target> <targetPath> insert <index> string block <sourcePos>
/data modify entity <target> <targetPath> insert <index> string block <sourcePos> <sourcePath>
/data modify entity <target> <targetPath> insert <index> string block <sourcePos> <sourcePath> <start>
/data modify entity <target> <targetPath> insert <index> string block <sourcePos> <sourcePath> <start> <end>
/data modify entity <target> <targetPath> insert <index> string entity <source>
/data modify entity <target> <targetPath> insert <index> string entity <source> <sourcePath>
/data modify entity <target> <targetPath> insert <index> string entity <source> <sourcePath> <start>
/data modify entity <target> <targetPath> insert <index> string entity <source> <sourcePath> <start> <end>
/data modify entity <target> <targetPath> insert <index> string storage <source>
/data modify entity <target> <targetPath> insert <index> string storage <source> <sourcePath>
/data modify entity <target> <targetPath> insert <index> string storage <source> <sourcePath> <start>
/data modify entity <target> <targetPath> insert <index> string storage <source> <sourcePath> <start> <end>
/data modify entity <target> <targetPath> insert <index> value <value>
/data modify entity <target> <targetPath> merge from block <sourcePos>
/data modify entity <target> <targetPath> merge from block <sourcePos> <sourcePath>
/data modify entity <target> <targetPath> merge from entity <source>
/data modify entity <target> <targetPath> merge from entity <source> <sourcePath>
/data modify entity <target> <targetPath> merge from storage <source>
/data modify entity <target> <targetPath> merge from storage <source> <sourcePath>
/data modify entity <target> <targetPath> merge string block <sourcePos>
/data modify entity <target> <targetPath> merge string block <sourcePos> <sourcePath>
/data modify entity <target> <targetPath> merge string block <sourcePos> <sourcePath> <start>
/data modify entity <target> <targetPath> merge string block <sourcePos> <sourcePath> <start> <end>
/data modify entity <target> <targetPath> merge string entity <source>
/data modify entity <target> <targetPath> merge string entity <source> <sourcePath>
/data modify entity <target> <targetPath> merge string entity <source> <sourcePath> <start>
/data modify entity <target> <targetPath> merge string entity <source> <sourcePath> <start> <end>
/data modify entity <target> <targetPath> merge string storage <source>
/data modify entity <target> <targetPath> merge string storage <source> <sourcePath>
/data modify entity <target> <targetPath> merge string storage <source> <sourcePath> <start>
/data modify entity <target> <targetPath> merge string storage <source> <sourcePath> <start> <end>
/data modify entity <target> <targetPath> merge value <value>
/data modify entity <target> <targetPath> prepend from block <sourcePos>
/data modify entity <target> <targetPath> prepend from block <sourcePos> <sourcePath>
/data modify entity <target> <targetPath> prepend from entity <source>
/data modify entity <target> <targetPath> prepend from entity <source> <sourcePath>
/data modify entity <target> <targetPath> prepend from storage <source>
/data modify entity <target> <targetPath> prepend from storage <source> <sourcePath>
/data modify entity <target> <targetPath> prepend string block <sourcePos>
/data modify entity <target> <targetPath> prepend string block <sourcePos> <sourcePath>
/data modify entity <target> <targetPath> prepend string block <sourcePos> <sourcePath> <start>
/data modify entity <target> <targetPath> prepend string block <sourcePos> <sourcePath> <start> <end>
/data modify entity <target> <targetPath> prepend string entity <source>
/data modify entity <target> <targetPath> prepend string entity <source> <sourcePath>
/data modify entity <target> <targetPath> prepend string entity <source> <sourcePath> <start>
/data modify entity <target> <targetPath> prepend string entity <source> <sourcePath> <start> <end>
/data modify entity <target> <targetPath> prepend string storage <source>
/data modify entity <target> <targetPath> prepend string storage <source> <sourcePath>
/data modify entity <target> <targetPath> prepend string storage <source> <sourcePath> <start>
/data modify entity <target> <targetPath> prepend string storage <source> <sourcePath> <start> <end>
/data modify entity <target> <targetPath> prepend value <value>
/data modify entity <target> <targetPath> set from block <sourcePos>
/data modify entity <target> <targetPath> set from block <sourcePos> <sourcePath>
/data modify entity <target> <targetPath> set from entity <source>
/data modify entity <target> <targetPath> set from entity <source> <sourcePath>
/data modify entity <target> <targetPath> set from storage <source>
/data modify entity <target> <targetPath> set from storage <source> <sourcePath>
/data modify entity <target> <targetPath> set string block <sourcePos>
/data modify entity <target> <targetPath> set string block <sourcePos> <sourcePath>
/data modify entity <target> <targetPath> set string block <sourcePos> <sourcePath> <start>
/data modify entity <target> <targetPath> set string block <sourcePos> <sourcePath> <start> <end>
/data modify entity <target> <targetPath> set string entity <source>
/data modify entity <target> <targetPath> set string entity <source> <sourcePath>
/data modify entity <target> <targetPath> set string entity <source> <sourcePath> <start>
/data modify entity <target> <targetPath> set string entity <source> <sourcePath> <start> <end>
/data modify entity <target> <targetPath> set string storage <source>
/data modify entity <target> <targetPath> set string storage <source> <sourcePath>
/data modify entity <target> <targetPath> set string storage <source> <sourcePath> <start>
/data modify entity <target> <targetPath> set string storage <source> <sourcePath> <start> <end>
/data modify entity <target> <targetPath> set value <value>
/data modify storage <target> <targetPath> append from block <sourcePos>
/data modify storage <target> <targetPath> append from block <sourcePos> <sourcePath>
/data modify storage <target> <targetPath> append from entity <source>
/data modify storage <target> <targetPath> append from entity <source> <sourcePath>
/data modify storage <target> <targetPath> append from storage <source>
/data modify storage <target> <targetPath> append from storage <source> <sourcePath>
/data modify storage <target> <targetPath> append string block <sourcePos>
/data modify storage <target> <targetPath> append string block <sourcePos> <sourcePath>
/data modify storage <target> <targetPath> append string block <sourcePos> <sourcePath> <start>
/data modify storage <target> <targetPath> append string block <sourcePos> <sourcePath> <start> <end>
/data modify storage <target> <targetPath> append string entity <source>
/data modify storage <target> <targetPath> append string entity <source> <sourcePath>
/data modify storage <target> <targetPath> append string entity <source> <sourcePath> <start>
/data modify storage <target> <targetPath> append string entity <source> <sourcePath> <start> <end>
/data modify storage <target> <targetPath> append string storage <source>
/data modify storage <target> <targetPath> append string storage <source> <sourcePath>
/data modify storage <target> <targetPath> append string storage <source> <sourcePath> <start>
/data modify storage <target> <targetPath> append string storage <source> <sourcePath> <start> <end>
/data modify storage <target> <targetPath> append value <value>
/data modify storage <target> <targetPath> insert <index> from block <sourcePos>
/data modify storage <target> <targetPath> insert <index> from block <sourcePos> <sourcePath>
/data modify storage <target> <targetPath> insert <index> from entity <source>
/data modify storage <target> <targetPath> insert <index> from entity <source> <sourcePath>
/data modify storage <target> <targetPath> insert <index> from storage <source>
/data modify storage <target> <targetPath> insert <index> from storage <source> <sourcePath>
/data modify storage <target> <targetPath> insert <index> string block <sourcePos>
/data modify storage <target> <targetPath> insert <index> string block <sourcePos> <sourcePath>
/data modify storage <target> <targetPath> insert <index> string block <sourcePos> <sourcePath> <start>
/data modify storage <target> <targetPath> insert <index> string block <sourcePos> <sourcePath> <start> <end>
/data modify storage <target> <targetPath> insert <index> string entity <source>
/data modify storage <target> <targetPath> insert <index> string entity <source> <sourcePath>
/data modify storage <target> <targetPath> insert <index> string entity <source> <sourcePath> <start>
/data modify storage <target> <targetPath> insert <index> string entity <source> <sourcePath> <start> <end>
/data modify storage <target> <targetPath> insert <index> string storage <source>
/data modify storage <target> <targetPath> insert <index> string storage <source> <sourcePath>
/data modify storage <target> <targetPath> insert <index> string storage <source> <sourcePath> <start>
/data modify storage <target> <targetPath> insert <index> string storage <source> <sourcePath> <start> <end>
/data modify storage <target> <targetPath> insert <index> value <value>
/data modify storage <target> <targetPath> merge from block <sourcePos>
/data modify storage <target> <targetPath> merge from block <sourcePos> <sourcePath>
/data modify storage <target> <targetPath> merge from entity <source>
/data modify storage <target> <targetPath> merge from entity <source> <sourcePath>
/data modify storage <target> <targetPath> merge from storage <source>
/data modify storage <target> <targetPath> merge from storage <source> <sourcePath>
/data modify storage <target> <targetPath> merge string block <sourcePos>
/data modify storage <target> <targetPath> merge string block <sourcePos> <sourcePath>
/data modify storage <target> <targetPath> merge string block <sourcePos> <sourcePath> <start>
/data modify storage <target> <targetPath> merge string block <sourcePos> <sourcePath> <start> <end>
/data modify storage <target> <targetPath> merge string entity <source>
/data modify storage <target> <targetPath> merge string entity <source> <sourcePath>
/data modify storage <target> <targetPath> merge string entity <source> <sourcePath> <start>
/data modify storage <target> <targetPath> merge string entity <source> <sourcePath> <start> <end>
/data modify storage <target> <targetPath> merge string storage <source>
/data modify storage <target> <targetPath> merge string storage <source> <sourcePath>
/data modify storage <target> <targetPath> merge string storage <source> <sourcePath> <start>
/data modify storage <target> <targetPath> merge string storage <source> <sourcePath> <start> <end>
/data modify storage <target> <targetPath> merge value <value>
/data modify storage <target> <targetPath> prepend from block <sourcePos>
/data modify storage <target> <targetPath> prepend from block <sourcePos> <sourcePath>
/data modify storage <target> <targetPath> prepend from entity <source>
/data modify storage <target> <targetPath> prepend from entity <source> <sourcePath>
/data modify storage <target> <targetPath> prepend from storage <source>
/data modify storage <target> <targetPath> prepend from storage <source> <sourcePath>
/data modify storage <target> <targetPath> prepend string block <sourcePos>
/data modify storage <target> <targetPath> prepend string block <sourcePos> <sourcePath>
/data modify storage <target> <targetPath> prepend string block <sourcePos> <sourcePath> <start>
/data modify storage <target> <targetPath> prepend string block <sourcePos> <sourcePath> <start> <end>
/data modify storage <target> <targetPath> prepend string entity <source>
/data modify storage <target> <targetPath> prepend string entity <source> <sourcePath>
/data modify storage <target> <targetPath> prepend string entity <source> <sourcePath> <start>
/data modify storage <target> <targetPath> prepend string entity <source> <sourcePath> <start> <end>
/data modify storage <target> <targetPath> prepend string storage <source>
/data modify storage <target> <targetPath> prepend string storage <source> <sourcePath>
/data modify storage <target> <targetPath> prepend string storage <source> <sourcePath> <start>
/data modify storage <target> <targetPath> prepend string storage <source> <sourcePath> <start> <end>
/data modify storage <target> <targetPath> prepend value <value>
/data modify storage <target> <targetPath> set from block <sourcePos>
/data modify storage <target> <targetPath> set from block <sourcePos> <sourcePath>
/data modify storage <target> <targetPath> set from entity <source>
/data modify storage <target> <targetPath> set from entity <source> <sourcePath>
/data modify storage <target> <targetPath> set from storage <source>
/data modify storage <target> <targetPath> set from storage <source> <sourcePath>
/data modify storage <target> <targetPath> set string block <sourcePos>
/data modify storage <target> <targetPath> set string block <sourcePos> <sourcePath>
/data modify storage <target> <targetPath> set string block <sourcePos> <sourcePath> <start>
/data modify storage <target> <targetPath> set string block <sourcePos> <sourcePath> <start> <end>
/data modify storage <target> <targetPath> set string entity <source>
/data modify storage <target> <targetPath> set string entity <source> <sourcePath>
/data modify storage <target> <targetPath> set string entity <source> <sourcePath> <start>
/data modify storage <target> <targetPath> set string entity <source> <sourcePath> <start> <end>
/data modify storage <target> <targetPath> set string storage <source>
/data modify storage <target> <targetPath> set string storage <source> <sourcePath>
/data modify storage <target> <targetPath> set string storage <source> <sourcePath> <start>
/data modify storage <target> <targetPath> set string storage <source> <sourcePath> <start> <end>
/data modify storage <target> <targetPath> set value <value>
/data remove block <targetPos> <path>
/data remove entity <target> <path>
/data remove storage <target> <path>
```

### /datapack

```text
/datapack disable <name>
/datapack enable <name>
/datapack enable <name> after <existing>
/datapack enable <name> before <existing>
/datapack enable <name> first
/datapack enable <name> last
/datapack list
/datapack list available
/datapack list enabled
```

### /debug

```text
/debug function <name>
/debug start
/debug stop
```

### /defaultgamemode

```text
/defaultgamemode <gamemode>
```

### /delchunks

```text
/delchunks
/delchunks <args>
```

### /delhome

```text
/delhome <nombre>
```

### /delpwarp

```text
/delpwarp <nombre>
```

### /deltree

```text
/deltree
/deltree <args>
```

### /deop

```text
/deop <targets>
```

### /desc

```text
/desc
/desc <args>
```

### /descend

```text
/descend
/descend <args>
```

### /difficulty

```text
/difficulty
/difficulty easy
/difficulty hard
/difficulty normal
/difficulty peaceful
```

### /ec

```text
/ec
```

### /effect

```text
/effect clear
/effect clear <targets>
/effect clear <targets> <effect>
/effect give <targets> <effect>
/effect give <targets> <effect> <seconds>
/effect give <targets> <effect> <seconds> <amplifier>
/effect give <targets> <effect> <seconds> <amplifier> <hideParticles>
/effect give <targets> <effect> infinite
/effect give <targets> <effect> infinite <amplifier>
/effect give <targets> <effect> infinite <amplifier> <hideParticles>
```

### /enchant

```text
/enchant <targets> <enchantment>
/enchant <targets> <enchantment> <level>
```

### /enderchest

```text
/enderchest
```

### /evs

```text
/evs
/evs <slot>
```

### /ex

```text
/ex
/ex <args>
```

### /execute

```text
/execute align <axes> -> execute
/execute anchored <anchor> -> execute
/execute as <targets> -> execute
/execute at <targets> -> execute
/execute facing <pos> -> execute
/execute facing entity <targets> <anchor> -> execute
/execute if biome <pos> <biome>
/execute if biome <pos> <biome> -> execute
/execute if block <pos> <block>
/execute if block <pos> <block> -> execute
/execute if blocks <start> <end> <destination> all
/execute if blocks <start> <end> <destination> all -> execute
/execute if blocks <start> <end> <destination> masked
/execute if blocks <start> <end> <destination> masked -> execute
/execute if data block <sourcePos> <path>
/execute if data block <sourcePos> <path> -> execute
/execute if data entity <source> <path>
/execute if data entity <source> <path> -> execute
/execute if data storage <source> <path>
/execute if data storage <source> <path> -> execute
/execute if dimension <dimension>
/execute if dimension <dimension> -> execute
/execute if entity <entities>
/execute if entity <entities> -> execute
/execute if function <name> -> execute
/execute if items block <pos> <slots> <item_predicate>
/execute if items block <pos> <slots> <item_predicate> -> execute
/execute if items entity <entities> <slots> <item_predicate>
/execute if items entity <entities> <slots> <item_predicate> -> execute
/execute if loaded <pos>
/execute if loaded <pos> -> execute
/execute if predicate <predicate>
/execute if predicate <predicate> -> execute
/execute if score <target> <targetObjective> < <source> <sourceObjective>
/execute if score <target> <targetObjective> < <source> <sourceObjective> -> execute
/execute if score <target> <targetObjective> <= <source> <sourceObjective>
/execute if score <target> <targetObjective> <= <source> <sourceObjective> -> execute
/execute if score <target> <targetObjective> = <source> <sourceObjective>
/execute if score <target> <targetObjective> = <source> <sourceObjective> -> execute
/execute if score <target> <targetObjective> > <source> <sourceObjective>
/execute if score <target> <targetObjective> > <source> <sourceObjective> -> execute
/execute if score <target> <targetObjective> >= <source> <sourceObjective>
/execute if score <target> <targetObjective> >= <source> <sourceObjective> -> execute
/execute if score <target> <targetObjective> matches <range>
/execute if score <target> <targetObjective> matches <range> -> execute
/execute in <dimension> -> execute
/execute on attacker -> execute
/execute on controller -> execute
/execute on leasher -> execute
/execute on origin -> execute
/execute on owner -> execute
/execute on passengers -> execute
/execute on target -> execute
/execute on vehicle -> execute
/execute positioned <pos> -> execute
/execute positioned as <targets> -> execute
/execute positioned over <heightmap> -> execute
/execute rotated <rot> -> execute
/execute rotated as <targets> -> execute
/execute run ...
/execute store result block <targetPos> <path> byte <scale> -> execute
/execute store result block <targetPos> <path> double <scale> -> execute
/execute store result block <targetPos> <path> float <scale> -> execute
/execute store result block <targetPos> <path> int <scale> -> execute
/execute store result block <targetPos> <path> long <scale> -> execute
/execute store result block <targetPos> <path> short <scale> -> execute
/execute store result bossbar <id> max -> execute
/execute store result bossbar <id> value -> execute
/execute store result entity <target> <path> byte <scale> -> execute
/execute store result entity <target> <path> double <scale> -> execute
/execute store result entity <target> <path> float <scale> -> execute
/execute store result entity <target> <path> int <scale> -> execute
/execute store result entity <target> <path> long <scale> -> execute
/execute store result entity <target> <path> short <scale> -> execute
/execute store result score <targets> <objective> -> execute
/execute store result storage <target> <path> byte <scale> -> execute
/execute store result storage <target> <path> double <scale> -> execute
/execute store result storage <target> <path> float <scale> -> execute
/execute store result storage <target> <path> int <scale> -> execute
/execute store result storage <target> <path> long <scale> -> execute
/execute store result storage <target> <path> short <scale> -> execute
/execute store success block <targetPos> <path> byte <scale> -> execute
/execute store success block <targetPos> <path> double <scale> -> execute
/execute store success block <targetPos> <path> float <scale> -> execute
/execute store success block <targetPos> <path> int <scale> -> execute
/execute store success block <targetPos> <path> long <scale> -> execute
/execute store success block <targetPos> <path> short <scale> -> execute
/execute store success bossbar <id> max -> execute
/execute store success bossbar <id> value -> execute
/execute store success entity <target> <path> byte <scale> -> execute
/execute store success entity <target> <path> double <scale> -> execute
/execute store success entity <target> <path> float <scale> -> execute
/execute store success entity <target> <path> int <scale> -> execute
/execute store success entity <target> <path> long <scale> -> execute
/execute store success entity <target> <path> short <scale> -> execute
/execute store success score <targets> <objective> -> execute
/execute store success storage <target> <path> byte <scale> -> execute
/execute store success storage <target> <path> double <scale> -> execute
/execute store success storage <target> <path> float <scale> -> execute
/execute store success storage <target> <path> int <scale> -> execute
/execute store success storage <target> <path> long <scale> -> execute
/execute store success storage <target> <path> short <scale> -> execute
/execute summon <entity> -> execute
/execute unless biome <pos> <biome>
/execute unless biome <pos> <biome> -> execute
/execute unless block <pos> <block>
/execute unless block <pos> <block> -> execute
/execute unless blocks <start> <end> <destination> all
/execute unless blocks <start> <end> <destination> all -> execute
/execute unless blocks <start> <end> <destination> masked
/execute unless blocks <start> <end> <destination> masked -> execute
/execute unless data block <sourcePos> <path>
/execute unless data block <sourcePos> <path> -> execute
/execute unless data entity <source> <path>
/execute unless data entity <source> <path> -> execute
/execute unless data storage <source> <path>
/execute unless data storage <source> <path> -> execute
/execute unless dimension <dimension>
/execute unless dimension <dimension> -> execute
/execute unless entity <entities>
/execute unless entity <entities> -> execute
/execute unless function <name> -> execute
/execute unless items block <pos> <slots> <item_predicate>
/execute unless items block <pos> <slots> <item_predicate> -> execute
/execute unless items entity <entities> <slots> <item_predicate>
/execute unless items entity <entities> <slots> <item_predicate> -> execute
/execute unless loaded <pos>
/execute unless loaded <pos> -> execute
/execute unless predicate <predicate>
/execute unless predicate <predicate> -> execute
/execute unless score <target> <targetObjective> < <source> <sourceObjective>
/execute unless score <target> <targetObjective> < <source> <sourceObjective> -> execute
/execute unless score <target> <targetObjective> <= <source> <sourceObjective>
/execute unless score <target> <targetObjective> <= <source> <sourceObjective> -> execute
/execute unless score <target> <targetObjective> = <source> <sourceObjective>
/execute unless score <target> <targetObjective> = <source> <sourceObjective> -> execute
/execute unless score <target> <targetObjective> > <source> <sourceObjective>
/execute unless score <target> <targetObjective> > <source> <sourceObjective> -> execute
/execute unless score <target> <targetObjective> >= <source> <sourceObjective>
/execute unless score <target> <targetObjective> >= <source> <sourceObjective> -> execute
/execute unless score <target> <targetObjective> matches <range>
/execute unless score <target> <targetObjective> matches <range> -> execute
```

### /experience

```text
/experience add <targets> <amount>
/experience add <targets> <amount> levels
/experience add <targets> <amount> points
/experience query <targets> levels
/experience query <targets> points
/experience set <targets> <amount>
/experience set <targets> <amount> levels
/experience set <targets> <amount> points
```

### /ext

```text
/ext
/ext <args>
```

### /extinguish

```text
/extinguish
/extinguish <args>
```

### /farwand

```text
/farwand
/farwand <args>
```

### /fill

```text
/fill <from> <to> <block>
/fill <from> <to> <block> destroy
/fill <from> <to> <block> hollow
/fill <from> <to> <block> keep
/fill <from> <to> <block> outline
/fill <from> <to> <block> replace
/fill <from> <to> <block> replace <filter>
```

### /fillbiome

```text
/fillbiome <from> <to> <biome>
/fillbiome <from> <to> <biome> replace <filter>
```

### /fixlava

```text
/fixlava
/fixlava <args>
```

### /fixwater

```text
/fixwater
/fixwater <args>
```

### /flood

```text
/flood
/flood <args>
```

### /floodfill

```text
/floodfill
/floodfill <args>
```

### /forceload

```text
/forceload add <from>
/forceload add <from> <to>
/forceload query
/forceload query <pos>
/forceload remove <from>
/forceload remove <from> <to>
/forceload remove all
```

### /forcespawn

```text
/forcespawn
/forcespawn <amount>
```

### /forestgen

```text
/forestgen
/forestgen <args>
```

### /freezepokemon

```text
/freezepokemon
/freezepokemon <seconds>
```

### /friendship

```text
/friendship <slot>
```

### /function

```text
/function <name>
/function <name> <arguments>
/function <name> with block <sourcePos>
/function <name> with block <sourcePos> <path>
/function <name> with entity <source>
/function <name> with entity <source> <path>
/function <name> with storage <source>
/function <name> with storage <source> <path>
```

### /gamemode

```text
/gamemode <gamemode>
/gamemode <gamemode> <target>
```

### /gamerule

```text
/gamerule accessories.keepAccessoryInventory
/gamerule accessories.keepAccessoryInventory <value>
/gamerule announceAdvancements
/gamerule announceAdvancements <value>
/gamerule axiomDoBlockDrops
/gamerule axiomDoBlockDrops <value>
/gamerule axiomDoBlockGravity
/gamerule axiomDoBlockGravity <value>
/gamerule axiomDoTrampleFarmland
/gamerule axiomDoTrampleFarmland <value>
/gamerule axiomPlayerInvulnerability
/gamerule axiomPlayerInvulnerability <value>
/gamerule battleInvulnerability
/gamerule battleInvulnerability <value>
/gamerule blockExplosionDropDecay
/gamerule blockExplosionDropDecay <value>
/gamerule commandBlockOutput
/gamerule commandBlockOutput <value>
/gamerule commandModificationBlockLimit
/gamerule commandModificationBlockLimit <value>
/gamerule consumeMoveItemOnUse
/gamerule consumeMoveItemOnUse <value>
/gamerule disableElytraMovementCheck
/gamerule disableElytraMovementCheck <value>
/gamerule disableRaids
/gamerule disableRaids <value>
/gamerule doDaylightCycle
/gamerule doDaylightCycle <value>
/gamerule doEntityDrops
/gamerule doEntityDrops <value>
/gamerule doFireTick
/gamerule doFireTick <value>
/gamerule doImmediateRespawn
/gamerule doImmediateRespawn <value>
/gamerule doInsomnia
/gamerule doInsomnia <value>
/gamerule doLimitedCrafting
/gamerule doLimitedCrafting <value>
/gamerule doMobLoot
/gamerule doMobLoot <value>
/gamerule doMobSpawning
/gamerule doMobSpawning <value>
/gamerule doPatrolSpawning
/gamerule doPatrolSpawning <value>
/gamerule doPokemonLoot
/gamerule doPokemonLoot <value>
/gamerule doPokemonSpawning
/gamerule doPokemonSpawning <value>
/gamerule doShinyStarters
/gamerule doShinyStarters <value>
/gamerule doTileDrops
/gamerule doTileDrops <value>
/gamerule doTraderSpawning
/gamerule doTraderSpawning <value>
/gamerule doVinesSpread
/gamerule doVinesSpread <value>
/gamerule doWardenSpawning
/gamerule doWardenSpawning <value>
/gamerule doWeatherCycle
/gamerule doWeatherCycle <value>
/gamerule drowningDamage
/gamerule drowningDamage <value>
/gamerule enderPearlsVanishOnDeath
/gamerule enderPearlsVanishOnDeath <value>
/gamerule fallDamage
/gamerule fallDamage <value>
/gamerule fireDamage
/gamerule fireDamage <value>
/gamerule forgiveDeadPlayers
/gamerule forgiveDeadPlayers <value>
/gamerule freezeDamage
/gamerule freezeDamage <value>
/gamerule globalSoundEvents
/gamerule globalSoundEvents <value>
/gamerule healersHealPC
/gamerule healersHealPC <value>
/gamerule keepInventory
/gamerule keepInventory <value>
/gamerule lavaSourceConversion
/gamerule lavaSourceConversion <value>
/gamerule logAdminCommands
/gamerule logAdminCommands <value>
/gamerule maxCommandChainLength
/gamerule maxCommandChainLength <value>
/gamerule maxCommandForkCount
/gamerule maxCommandForkCount <value>
/gamerule maxEntityCramming
/gamerule maxEntityCramming <value>
/gamerule mobExplosionDropDecay
/gamerule mobExplosionDropDecay <value>
/gamerule mobGriefing
/gamerule mobGriefing <value>
/gamerule mobTargetInBattle
/gamerule mobTargetInBattle <value>
/gamerule naturalRegeneration
/gamerule naturalRegeneration <value>
/gamerule playersNetherPortalCreativeDelay
/gamerule playersNetherPortalCreativeDelay <value>
/gamerule playersNetherPortalDefaultDelay
/gamerule playersNetherPortalDefaultDelay <value>
/gamerule playersSleepingPercentage
/gamerule playersSleepingPercentage <value>
/gamerule projectilesCanBreakBlocks
/gamerule projectilesCanBreakBlocks <value>
/gamerule randomTickSpeed
/gamerule randomTickSpeed <value>
/gamerule reducedDebugInfo
/gamerule reducedDebugInfo <value>
/gamerule sendCommandFeedback
/gamerule sendCommandFeedback <value>
/gamerule showDeathMessages
/gamerule showDeathMessages <value>
/gamerule snowAccumulationHeight
/gamerule snowAccumulationHeight <value>
/gamerule spawnChunkRadius
/gamerule spawnChunkRadius <value>
/gamerule spawnRadius
/gamerule spawnRadius <value>
/gamerule spectatorsGenerateChunks
/gamerule spectatorsGenerateChunks <value>
/gamerule tntExplosionDropDecay
/gamerule tntExplosionDropDecay <value>
/gamerule universalAnger
/gamerule universalAnger <value>
/gamerule waterSourceConversion
/gamerule waterSourceConversion <value>
```

### /getnbt

```text
/getnbt
```

### /give

```text
/give <targets> <item>
/give <targets> <item> <count>
```

### /giveallmarks

```text
/giveallmarks <player> <slot>
```

### /giveallpokemon

```text
/giveallpokemon
/giveallpokemon <min>
/giveallpokemon <min> <max>
```

### /givemark

```text
/givemark <player> <slot> <mark>
```

### /givepokemon

```text
/givepokemon <properties>
```

### /givepokemonegg

```text
/givepokemonegg <player> <properties>
```

### /givepokemonother

```text
/givepokemonother <player> <properties>
```

### /givetm

```text
/givetm <move>
/givetm <move> <player>
/givetm <move> <player> <quantity>
/givetm <move> <quantity>
```

### /gmask

```text
/gmask
/gmask <args>
```

### /green

```text
/green
/green <args>
```

### /grindstone

```text
/grindstone
```

### /hatchpokemonegg

```text
/hatchpokemonegg <player>
```

### /heal

```text
/heal
```

### /healpokemon

```text
/healpokemon
/healpokemon <player>
```

### /held_item

```text
/held_item <target> <slot> <item>
```

### /help

```text
/help
/help <command>
```

### /home

```text
/home
/home <nombre>
```

### /homes

```text
/homes
```

### /image2map

```text
/image2map create <mode> <path>
/image2map create <width> <height> <mode> <path>
/image2map preview <path>
```

### /info

```text
/info
/info <args>
```

### /islasnaranja

```text
/islasnaranja
```

### /item

```text
/item modify block <pos> <slot> <modifier>
/item modify entity <targets> <slot> <modifier>
/item replace block <pos> <slot> from block <source> <sourceSlot>
/item replace block <pos> <slot> from block <source> <sourceSlot> <modifier>
/item replace block <pos> <slot> from entity <source> <sourceSlot>
/item replace block <pos> <slot> from entity <source> <sourceSlot> <modifier>
/item replace block <pos> <slot> with <item>
/item replace block <pos> <slot> with <item> <count>
/item replace entity <targets> <slot> from block <source> <sourceSlot>
/item replace entity <targets> <slot> from block <source> <sourceSlot> <modifier>
/item replace entity <targets> <slot> from entity <source> <sourceSlot>
/item replace entity <targets> <slot> from entity <source> <sourceSlot> <modifier>
/item replace entity <targets> <slot> with <item>
/item replace entity <targets> <slot> with <item> <count>
```

### /ivs

```text
/ivs
/ivs <slot>
```

### /j

```text
/j
/j <args>
```

### /jade

```text
/jade hide <targets>
/jade show <targets>
```

### /jfr

```text
/jfr start
/jfr stop
```

### /jumpto

```text
/jumpto
/jumpto <args>
```

### /kick

```text
/kick <targets>
/kick <targets> <reason>
```

### /kill

```text
/kill
/kill <targets>
```

### /kits

```text
/kits rotacion reset
/kits rotacion reset <dias>
```

### /l

```text
/l l -> login
```

### /letmedespawn

```text
/letmedespawn add <mobName>
/letmedespawn addPersistenceEnabler <itemName>
/letmedespawn remove <mobName>
/letmedespawn removePersistenceEnabler <itemName>
```

### /levelup

```text
/levelup <player> <slot>
/levelup <slot>
```

### /list

```text
/list
/list uuids
```

### /listchunks

```text
/listchunks
/listchunks <args>
```

### /locate

```text
/locate biome <biome>
/locate poi <poi>
/locate structure <structure>
```

### /login

```text
/login
/login <password>
```

### /logout

```text
/logout
```

### /loot

```text
/loot give <players> fish <loot_table> <pos>
/loot give <players> fish <loot_table> <pos> <tool>
/loot give <players> fish <loot_table> <pos> mainhand
/loot give <players> fish <loot_table> <pos> offhand
/loot give <players> kill <target>
/loot give <players> loot <loot_table>
/loot give <players> mine <pos>
/loot give <players> mine <pos> <tool>
/loot give <players> mine <pos> mainhand
/loot give <players> mine <pos> offhand
/loot insert <targetPos> fish <loot_table> <pos>
/loot insert <targetPos> fish <loot_table> <pos> <tool>
/loot insert <targetPos> fish <loot_table> <pos> mainhand
/loot insert <targetPos> fish <loot_table> <pos> offhand
/loot insert <targetPos> kill <target>
/loot insert <targetPos> loot <loot_table>
/loot insert <targetPos> mine <pos>
/loot insert <targetPos> mine <pos> <tool>
/loot insert <targetPos> mine <pos> mainhand
/loot insert <targetPos> mine <pos> offhand
/loot replace block <targetPos> <slot> <count> fish <loot_table> <pos>
/loot replace block <targetPos> <slot> <count> fish <loot_table> <pos> <tool>
/loot replace block <targetPos> <slot> <count> fish <loot_table> <pos> mainhand
/loot replace block <targetPos> <slot> <count> fish <loot_table> <pos> offhand
/loot replace block <targetPos> <slot> <count> kill <target>
/loot replace block <targetPos> <slot> <count> loot <loot_table>
/loot replace block <targetPos> <slot> <count> mine <pos>
/loot replace block <targetPos> <slot> <count> mine <pos> <tool>
/loot replace block <targetPos> <slot> <count> mine <pos> mainhand
/loot replace block <targetPos> <slot> <count> mine <pos> offhand
/loot replace block <targetPos> <slot> fish <loot_table> <pos>
/loot replace block <targetPos> <slot> fish <loot_table> <pos> <tool>
/loot replace block <targetPos> <slot> fish <loot_table> <pos> mainhand
/loot replace block <targetPos> <slot> fish <loot_table> <pos> offhand
/loot replace block <targetPos> <slot> kill <target>
/loot replace block <targetPos> <slot> loot <loot_table>
/loot replace block <targetPos> <slot> mine <pos>
/loot replace block <targetPos> <slot> mine <pos> <tool>
/loot replace block <targetPos> <slot> mine <pos> mainhand
/loot replace block <targetPos> <slot> mine <pos> offhand
/loot replace entity <entities> <slot> <count> fish <loot_table> <pos>
/loot replace entity <entities> <slot> <count> fish <loot_table> <pos> <tool>
/loot replace entity <entities> <slot> <count> fish <loot_table> <pos> mainhand
/loot replace entity <entities> <slot> <count> fish <loot_table> <pos> offhand
/loot replace entity <entities> <slot> <count> kill <target>
/loot replace entity <entities> <slot> <count> loot <loot_table>
/loot replace entity <entities> <slot> <count> mine <pos>
/loot replace entity <entities> <slot> <count> mine <pos> <tool>
/loot replace entity <entities> <slot> <count> mine <pos> mainhand
/loot replace entity <entities> <slot> <count> mine <pos> offhand
/loot replace entity <entities> <slot> fish <loot_table> <pos>
/loot replace entity <entities> <slot> fish <loot_table> <pos> <tool>
/loot replace entity <entities> <slot> fish <loot_table> <pos> mainhand
/loot replace entity <entities> <slot> fish <loot_table> <pos> offhand
/loot replace entity <entities> <slot> kill <target>
/loot replace entity <entities> <slot> loot <loot_table>
/loot replace entity <entities> <slot> mine <pos>
/loot replace entity <entities> <slot> mine <pos> <tool>
/loot replace entity <entities> <slot> mine <pos> mainhand
/loot replace entity <entities> <slot> mine <pos> offhand
/loot spawn <targetPos> fish <loot_table> <pos>
/loot spawn <targetPos> fish <loot_table> <pos> <tool>
/loot spawn <targetPos> fish <loot_table> <pos> mainhand
/loot spawn <targetPos> fish <loot_table> <pos> offhand
/loot spawn <targetPos> kill <target>
/loot spawn <targetPos> loot <loot_table>
/loot spawn <targetPos> mine <pos>
/loot spawn <targetPos> mine <pos> <tool>
/loot spawn <targetPos> mine <pos> mainhand
/loot spawn <targetPos> mine <pos> offhand
```

### /lrbuild

```text
/lrbuild
/lrbuild <args>
```

### /lumyreward

```text
/lumyreward <player> <champion>
```

### /luna

```text
/luna
/luna afk
/luna afk probar
/luna auditar
/luna autotest
/luna comercio
/luna constructor <clave>
/luna cosmeticos
/luna cosmeticos huerfanos
/luna dar <moneda> <cantidad>
/luna darmedalla <cual>
/luna darmedalla <cual> <jugador>
/luna darmedallas
/luna darmedallas <jugador>
/luna decorar <especie> <postura>
/luna decorar <especie> <postura> <donde>
/luna decorar <especie> <postura> <donde> <grados>
/luna decorar quitar
/luna decorar quitar <radio>
/luna economia
/luna economia <horas>
/luna enfermera npc
/luna estado
/luna gimnasio
/luna gimnasio <cual>
/luna gimnasio <cual> comprobar
/luna gimnasio <cual> equipo
/luna gimnasio <cual> liberar
/luna gimnasio <cual> lider
/luna gimnasio <cual> limpiarranuras
/luna gimnasio <cual> medir
/luna gimnasio <cual> mirar <grados>
/luna gimnasio <cual> nomirar
/luna gimnasio <cual> plataforma
/luna gimnasio <cual> posiciones
/luna gimnasio <cual> purgar
/luna gimnasio <cual> testadaptador
/luna gimnasio ciudadela
/luna gimnasio ciudadela <grados>
/luna gimnasio ciudadela quitar
/luna gimnasio liberar
/luna gimnasio plataformas
/luna gimnasio reclonar
/luna inicial
/luna inicial abrir <jugador>
/luna inicial dar <jugador> <especie>
/luna inicial oak
/luna inicial oak quitar
/luna inicial reiniciar
/luna inicial reiniciar <jugador>
/luna ir <destino>
/luna kits rotacion reset
/luna kits rotacion reset <dias>
/luna lunita debug
/luna lunita goto
/luna lunita respawn
/luna lunita status
/luna lunita tp
/luna naranja plataforma
/luna naranja plataforma <radio>
/luna paradas
/luna paradas etiquetar
/luna paradas quitar
/luna pase
/luna pase nivel <jugador> <n>
/luna pase nueva_temporada
/luna pase nueva_temporada <dias>
/luna pase reiniciar <jugador>
/luna pase via_luna <jugador>
/luna pase xp <jugador> <cantidad>
/luna probar <gimnasio>
/luna puerta
/luna puerta activar
/luna puerta desactivar
/luna puerta npc
/luna puerta npc <especie>
/luna puerta npc quitar
/luna puerta reiniciar <jugador>
/luna quienpokemon estado
/luna quienpokemon iniciar
/luna rango
/luna rango <jugador> <rango>
/luna reiniciarinicial
/luna reiniciarmedalla <cual>
/luna reiniciarmedalla <cual> <jugador>
/luna reiniciarmision <id>
/luna rotarcazas
/luna rotom reset
/luna rotom spawn
/luna rotom testtorre
/luna rotom testvideo
/luna rotom tp
/luna rotom tptorre
/luna saldo
/luna santuario aprobar <foto>
/luna santuario eliminar <nicho>
/luna santuario info <nicho>
/luna santuario listar
/luna santuario nicho
/luna santuario nicho aqui
/luna santuario nicho aqui <nombre>
/luna santuario nicho borrar <nicho>
/luna santuario nicho ir <nicho>
/luna santuario nicho proyector <nicho>
/luna santuario nicho recolocar
/luna santuario nicho renombrar <nicho> <nombre>
/luna santuario nicho ver
/luna santuario npc
/luna santuario npc quitar
/luna santuario pendientes
/luna santuario recargar
/luna santuario rechazar <foto>
/luna tebex chargeback <transaction> <uuid> <packageId>
/luna tebex fulfill <transaction> <uuid> <packageId> <purchaseQuantity>
/luna tebex fulfill <transaction> <uuid> <packageId> <purchaseQuantity> <username>
/luna tebex refund <transaction> <uuid> <packageId>
/luna testrango <rango>
/luna torre_batalla arena
/luna torre_batalla dar_ronda <jugador> <modo> <ronda>
/luna torre_batalla holograma 1vs1
/luna torre_batalla holograma 2vs2
/luna torre_batalla holograma actualizar
/luna torre_batalla holograma aleatorio
/luna torre_batalla holograma quitar
/luna torre_batalla npc
/luna torre_batalla npc quitar
/luna torre_batalla nueva_temporada
/luna torre_batalla tp
/luna traje <jugador>
/luna traje dar <jugador> <traje>
/luna traje quitar <jugador> <traje>
/luna via <via> <xp>
/luna videooak
```

### /lunita

```text
/lunita dar <jugador>
/lunita dar <jugador> <nivel>
/lunita verificar
```

### /mask

```text
/mask
/mask <args>
```

### /material

```text
/material
/material <args>
```

### /me

```text
/me <action>
```

### /midnightconfig

```text
/midnightconfig cobblemon-cards allowFakemonCards
/midnightconfig cobblemon-cards allowFakemonCards <value>
/midnightconfig cobblemon-cards armorStatMultiplier
/midnightconfig cobblemon-cards armorStatMultiplier <value>
/midnightconfig cobblemon-cards attackDamageStatMultiplier
/midnightconfig cobblemon-cards attackDamageStatMultiplier <value>
/midnightconfig cobblemon-cards attackSpeedStatMultiplier
/midnightconfig cobblemon-cards attackSpeedStatMultiplier <value>
/midnightconfig cobblemon-cards boosterChestSpawnChance
/midnightconfig cobblemon-cards boosterChestSpawnChance <value>
/midnightconfig cobblemon-cards cardDropChance
/midnightconfig cobblemon-cards cardDropChance <value>
/midnightconfig cobblemon-cards cardDropChanceStatMultiplier
/midnightconfig cobblemon-cards cardDropChanceStatMultiplier <value>
/midnightconfig cobblemon-cards diamondBinderPages
/midnightconfig cobblemon-cards diamondBinderPages <value>
/midnightconfig cobblemon-cards displayPercentStatOnCards
/midnightconfig cobblemon-cards displayPercentStatOnCards <value>
/midnightconfig cobblemon-cards enableBoosterChestSpawn
/midnightconfig cobblemon-cards enableBoosterChestSpawn <value>
/midnightconfig cobblemon-cards enableCardStats
/midnightconfig cobblemon-cards enableCardStats <value>
/midnightconfig cobblemon-cards enableDiamondBinder
/midnightconfig cobblemon-cards enableDiamondBinder <value>
/midnightconfig cobblemon-cards enableGoldBinder
/midnightconfig cobblemon-cards enableGoldBinder <value>
/midnightconfig cobblemon-cards enableIronBinder
/midnightconfig cobblemon-cards enableIronBinder <value>
/midnightconfig cobblemon-cards enableLeatherBinder
/midnightconfig cobblemon-cards enableLeatherBinder <value>
/midnightconfig cobblemon-cards enableMasterAlbum
/midnightconfig cobblemon-cards enableMasterAlbum <value>
/midnightconfig cobblemon-cards enableNetheriteBinder
/midnightconfig cobblemon-cards enableNetheriteBinder <value>
/midnightconfig cobblemon-cards enablePlayerStats
/midnightconfig cobblemon-cards enablePlayerStats <value>
/midnightconfig cobblemon-cards enableSpawnBoostStats
/midnightconfig cobblemon-cards enableSpawnBoostStats <value>
/midnightconfig cobblemon-cards globalStatMultiplier
/midnightconfig cobblemon-cards globalStatMultiplier <value>
/midnightconfig cobblemon-cards godPackTicketChance
/midnightconfig cobblemon-cards godPackTicketChance <value>
/midnightconfig cobblemon-cards goldBinderPages
/midnightconfig cobblemon-cards goldBinderPages <value>
/midnightconfig cobblemon-cards gradingStationDustCost
/midnightconfig cobblemon-cards gradingStationDustCost <value>
/midnightconfig cobblemon-cards gradingStationProcessTime
/midnightconfig cobblemon-cards gradingStationProcessTime <value>
/midnightconfig cobblemon-cards ironBinderPages
/midnightconfig cobblemon-cards ironBinderPages <value>
/midnightconfig cobblemon-cards leatherBinderPages
/midnightconfig cobblemon-cards leatherBinderPages <value>
/midnightconfig cobblemon-cards luckStatMultiplier
/midnightconfig cobblemon-cards luckStatMultiplier <value>
/midnightconfig cobblemon-cards lunaGoldGodPackChance
/midnightconfig cobblemon-cards lunaGoldGodPackChance <value>
/midnightconfig cobblemon-cards masterAlbumPages
/midnightconfig cobblemon-cards masterAlbumPages <value>
/midnightconfig cobblemon-cards maxHealthStatMultiplier
/midnightconfig cobblemon-cards maxHealthStatMultiplier <value>
/midnightconfig cobblemon-cards maxNationalDex
/midnightconfig cobblemon-cards maxNationalDex <value>
/midnightconfig cobblemon-cards maxSpawnBoostMultiplier
/midnightconfig cobblemon-cards maxSpawnBoostMultiplier <value>
/midnightconfig cobblemon-cards miningSpeedStatMultiplier
/midnightconfig cobblemon-cards miningSpeedStatMultiplier <value>
/midnightconfig cobblemon-cards movementSpeedStatMultiplier
/midnightconfig cobblemon-cards movementSpeedStatMultiplier <value>
/midnightconfig cobblemon-cards netheriteBinderPages
/midnightconfig cobblemon-cards netheriteBinderPages <value>
/midnightconfig cobblemon-cards playerStatMultiplier
/midnightconfig cobblemon-cards playerStatMultiplier <value>
/midnightconfig cobblemon-cards recyclerProcessTime
/midnightconfig cobblemon-cards recyclerProcessTime <value>
/midnightconfig cobblemon-cards restorerBaseCost
/midnightconfig cobblemon-cards restorerBaseCost <value>
/midnightconfig cobblemon-cards spawnBoostStatMultiplier
/midnightconfig cobblemon-cards spawnBoostStatMultiplier <value>
```

### /msd

```text
/msd apply <type> <resource_id>
/msd hard_reset
/msd reload
```

### /msg

```text
/msg <targets> <message>
```

### /naranja

```text
/naranja
```

### /navwand

```text
/navwand
/navwand <args>
```

### /none

```text
/none
/none <args>
```

### /npcdelete

```text
/npcdelete
/npcdelete <target>
```

### /npcedit

```text
/npcedit
```

### /npcspawn

```text
/npcspawn <class>
/npcspawn <class> <level>
```

### /npcspawnat

```text
/npcspawnat <pos> <class>
/npcspawnat <pos> <class> <level>
```

### /op

```text
/op <targets>
```

### /opendialogue

```text
/opendialogue <dialogue> <player>
```

### /openstarterscreen

```text
/openstarterscreen <player>
```

### /pardon

```text
/pardon <targets>
```

### /pardon-ip

```text
/pardon-ip <target>
```

### /particle

```text
/particle <name>
/particle <name> <pos>
/particle <name> <pos> <delta> <speed> <count>
/particle <name> <pos> <delta> <speed> <count> force
/particle <name> <pos> <delta> <speed> <count> force <viewers>
/particle <name> <pos> <delta> <speed> <count> normal
/particle <name> <pos> <delta> <speed> <count> normal <viewers>
```

### /pc

```text
/pc
/pc <box>
```

### /pcsearch

```text
/pcsearch <properties>
/pcsearch other <other> <properties>
```

### /pctake

```text
/pctake <player> <box> <slot>
```

### /perf

```text
/perf start
/perf stop
```

### /pickaxe

```text
/pickaxe
/pickaxe <args>
```

### /place

```text
/place feature <feature>
/place feature <feature> <pos>
/place jigsaw <pool> <target> <max_depth>
/place jigsaw <pool> <target> <max_depth> <position>
/place structure <structure>
/place structure <structure> <pos>
/place template <template>
/place template <template> <pos>
/place template <template> <pos> <rotation>
/place template <template> <pos> <rotation> <mirror>
/place template <template> <pos> <rotation> <mirror> <integrity>
/place template <template> <pos> <rotation> <mirror> <integrity> <seed>
```

### /placement

```text
/placement
/placement <args>
```

### /playsound

```text
/playsound <sound>
/playsound <sound> ambient
/playsound <sound> ambient <targets>
/playsound <sound> ambient <targets> <pos>
/playsound <sound> ambient <targets> <pos> <volume>
/playsound <sound> ambient <targets> <pos> <volume> <pitch>
/playsound <sound> ambient <targets> <pos> <volume> <pitch> <minVolume>
/playsound <sound> block
/playsound <sound> block <targets>
/playsound <sound> block <targets> <pos>
/playsound <sound> block <targets> <pos> <volume>
/playsound <sound> block <targets> <pos> <volume> <pitch>
/playsound <sound> block <targets> <pos> <volume> <pitch> <minVolume>
/playsound <sound> hostile
/playsound <sound> hostile <targets>
/playsound <sound> hostile <targets> <pos>
/playsound <sound> hostile <targets> <pos> <volume>
/playsound <sound> hostile <targets> <pos> <volume> <pitch>
/playsound <sound> hostile <targets> <pos> <volume> <pitch> <minVolume>
/playsound <sound> master
/playsound <sound> master <targets>
/playsound <sound> master <targets> <pos>
/playsound <sound> master <targets> <pos> <volume>
/playsound <sound> master <targets> <pos> <volume> <pitch>
/playsound <sound> master <targets> <pos> <volume> <pitch> <minVolume>
/playsound <sound> music
/playsound <sound> music <targets>
/playsound <sound> music <targets> <pos>
/playsound <sound> music <targets> <pos> <volume>
/playsound <sound> music <targets> <pos> <volume> <pitch>
/playsound <sound> music <targets> <pos> <volume> <pitch> <minVolume>
/playsound <sound> neutral
/playsound <sound> neutral <targets>
/playsound <sound> neutral <targets> <pos>
/playsound <sound> neutral <targets> <pos> <volume>
/playsound <sound> neutral <targets> <pos> <volume> <pitch>
/playsound <sound> neutral <targets> <pos> <volume> <pitch> <minVolume>
/playsound <sound> player
/playsound <sound> player <targets>
/playsound <sound> player <targets> <pos>
/playsound <sound> player <targets> <pos> <volume>
/playsound <sound> player <targets> <pos> <volume> <pitch>
/playsound <sound> player <targets> <pos> <volume> <pitch> <minVolume>
/playsound <sound> record
/playsound <sound> record <targets>
/playsound <sound> record <targets> <pos>
/playsound <sound> record <targets> <pos> <volume>
/playsound <sound> record <targets> <pos> <volume> <pitch>
/playsound <sound> record <targets> <pos> <volume> <pitch> <minVolume>
/playsound <sound> voice
/playsound <sound> voice <targets>
/playsound <sound> voice <targets> <pos>
/playsound <sound> voice <targets> <pos> <volume>
/playsound <sound> voice <targets> <pos> <volume> <pitch>
/playsound <sound> voice <targets> <pos> <volume> <pitch> <minVolume>
/playsound <sound> weather
/playsound <sound> weather <targets>
/playsound <sound> weather <targets> <pos>
/playsound <sound> weather <targets> <pos> <volume>
/playsound <sound> weather <targets> <pos> <volume> <pitch>
/playsound <sound> weather <targets> <pos> <volume> <pitch> <minVolume>
```

### /pokebox

```text
/pokebox <player> <slot>
/pokebox <player> <slot> <box>
```

### /pokeboxall

```text
/pokeboxall <player>
/pokeboxall <player> <box>
```

### /pokedex

```text
/pokedex grant <player> all <dex>
/pokedex grant <player> only <species> <form>
/pokedex printcalculations <player>
/pokedex printcalculations <player> <dex>
/pokedex printcalculations <player> all
/pokedex revoke <player> all <dex>
/pokedex revoke <player> only <species> <form>
```

### /pokeedit

```text
/pokeedit <slot> <properties>
```

### /pokeeditother

```text
/pokeeditother <player> <slot> <properties>
```

### /pokeevs

```text
/pokeevs
/pokeevs <slot>
```

### /pokegive

```text
/pokegive <properties>
```

### /pokegiveother

```text
/pokegiveother <player> <properties>
```

### /pokeheal

```text
/pokeheal
/pokeheal <player>
```

### /pokeivs

```text
/pokeivs
/pokeivs <slot>
```

### /pokemonedit

```text
/pokemonedit <slot> <properties>
```

### /pokemoneditother

```text
/pokemoneditother <player> <slot> <properties>
```

### /pokemonrestart

```text
/pokemonrestart
/pokemonrestart <reset_starters>
```

### /pokemonrestartother

```text
/pokemonrestartother <player>
/pokemonrestartother <player> <reset_starters>
```

### /pokenav

```text
/pokenav catalogue <player> grant all
/pokenav catalogue <player> grant id <id>
/pokenav catalogue <player> grant species <species>
/pokenav catalogue <player> list
/pokenav catalogue <player> revoke all
/pokenav catalogue <player> revoke id <id>
/pokenav catalogue <player> revoke species <species>
```

### /pokerestart

```text
/pokerestart
/pokerestart <reset_starters>
```

### /pokerestartother

```text
/pokerestartother <player>
/pokerestartother <player> <reset_starters>
```

### /pokespawn

```text
/pokespawn <properties>
```

### /pokespawnat

```text
/pokespawnat <pos> <properties>
```

### /pradmin

```text
/pradmin commands
/pradmin editmode off
/pradmin editmode on
/pradmin status
```

### /pumpkins

```text
/pumpkins
/pumpkins <args>
```

### /pwarp

```text
/pwarp
/pwarp <creador> <nombre>
```

### /pwarps

```text
/pwarps
```

### /querylearnset

```text
/querylearnset <player> <slot> <move>
```

### /random

```text
/random reset *
/random reset * <seed>
/random reset * <seed> <includeWorldSeed>
/random reset * <seed> <includeWorldSeed> <includeSequenceId>
/random reset <sequence>
/random reset <sequence> <seed>
/random reset <sequence> <seed> <includeWorldSeed>
/random reset <sequence> <seed> <includeWorldSeed> <includeSequenceId>
/random roll <range>
/random roll <range> <sequence>
/random value <range>
/random value <range> <sequence>
```

### /range

```text
/range
/range <args>
```

### /rctmod

```text
/rctmod player add progress <targets> after <trainerId>
/rctmod player add progress <targets> before <trainerId>
/rctmod player add progress after <trainerId>
/rctmod player add progress before <trainerId>
/rctmod player get defeats <trainerId>
/rctmod player get defeats <trainerId> <target>
/rctmod player get level_cap
/rctmod player get level_cap <target>
/rctmod player get luck
/rctmod player get luck <target>
/rctmod player get progress
/rctmod player get progress <target>
/rctmod player get progress <target> graph
/rctmod player get progress <target> graph <graph_flags>
/rctmod player get progress graph
/rctmod player get progress graph <graph_flags>
/rctmod player get series
/rctmod player get series <target>
/rctmod player get series <target> completed
/rctmod player get series completed
/rctmod player get type_defeats <type>
/rctmod player get type_defeats <type> <target>
/rctmod player set defeats <trainerId> <targets> <value>
/rctmod player set defeats <trainerId> <value>
/rctmod player set progress <targets> after <trainerId>
/rctmod player set progress <targets> before <trainerId>
/rctmod player set progress after <trainerId>
/rctmod player set progress before <trainerId>
/rctmod player set series <seriesId>
/rctmod player set series <seriesId> <targets>
/rctmod player set series <seriesId> <targets> completed <count>
/rctmod player set series <seriesId> completed <count>
/rctmod trainer get max_trainer_defeats <trainer>
/rctmod trainer get max_trainer_wins <trainer>
/rctmod trainer get required_defeats <trainer>
/rctmod trainer get required_level_cap <trainer> <player>
/rctmod trainer get required_series <trainer>
/rctmod trainer get reward_level_cap <trainer> <player>
/rctmod trainer get type <trainer>
/rctmod trainer spawn_for
/rctmod trainer spawn_for <target>
/rctmod trainer summon <trainer>
/rctmod trainer summon <trainer> <at>
/rctmod trainer summon <trainer> <at> <nbt>
/rctmod trainer summon <trainer> <nbt>
/rctmod trainer summon_persistent <trainer>
/rctmod trainer summon_persistent <trainer> <at>
/rctmod trainer summon_persistent <trainer> <at> <nbt>
/rctmod trainer summon_persistent <trainer> <nbt>
/rctmod trainer unregister_persistent <mobUUID>
```

### /recipe

```text
/recipe give <targets> *
/recipe give <targets> <recipe>
/recipe take <targets> *
/recipe take <targets> <recipe>
```

### /redo

```text
/redo
/redo <args>
```

### /reg

```text
/reg reg -> register
```

### /register

```text
/register
/register <password> <passwordAgain>
```

### /reload

```text
/reload
```

### /reloadshowdown

```text
/reloadshowdown
```

### /rem

```text
/rem
/rem <args>
```

### /rement

```text
/rement
/rement <args>
```

### /remove

```text
/remove
/remove <args>
```

### /removeabove

```text
/removeabove
/removeabove <args>
```

### /removebelow

```text
/removebelow
/removebelow <args>
```

### /removenear

```text
/removenear
/removenear <args>
```

### /rename

```text
/rename <nombre>
```

### /renamebox

```text
/renamebox <player> <box> <name>
```

### /repl

```text
/repl
/repl <args>
```

### /replacenear

```text
/replacenear
/replacenear <args>
```

### /restore

```text
/restore
/restore <args>
```

### /return

```text
/return <value>
/return fail
/return run ...
```

### /ride

```text
/ride <target> dismount
/ride <target> mount <vehicle>
```

### /runmolang

```text
/runmolang <molang>
/runmolang <molang> <npc>
/runmolang <molang> <npc> <pokemon>
/runmolang <molang> <player>
/runmolang <molang> <player> <npc>
/runmolang <molang> <player> <npc> <pokemon>
/runmolang <molang> <player> <pokemon>
/runmolang <molang> <pokemon>
```

### /runmolangscript

```text
/runmolangscript <script>
/runmolangscript <script> <npc>
/runmolangscript <script> <npc> <pokemon>
/runmolangscript <script> <player>
/runmolangscript <script> <player> <npc>
/runmolangscript <script> <player> <npc> <pokemon>
/runmolangscript <script> <player> <pokemon>
/runmolangscript <script> <pokemon>
```

### /save-all

```text
/save-all
/save-all flush
```

### /save-off

```text
/save-off
```

### /save-on

```text
/save-on
```

### /say

```text
/say <message>
```

### /schedule

```text
/schedule clear <function>
/schedule function <function> <time>
/schedule function <function> <time> append
/schedule function <function> <time> replace
```

### /schem

```text
/schem
/schem <args>
```

### /schematic

```text
/schematic
/schematic <args>
```

### /scoreboard

```text
/scoreboard objectives add <objective> <criteria>
/scoreboard objectives add <objective> <criteria> <displayName>
/scoreboard objectives list
/scoreboard objectives modify <objective> displayautoupdate <value>
/scoreboard objectives modify <objective> displayname <displayName>
/scoreboard objectives modify <objective> numberformat
/scoreboard objectives modify <objective> numberformat blank
/scoreboard objectives modify <objective> numberformat fixed <contents>
/scoreboard objectives modify <objective> numberformat styled <style>
/scoreboard objectives modify <objective> rendertype hearts
/scoreboard objectives modify <objective> rendertype integer
/scoreboard objectives remove <objective>
/scoreboard objectives setdisplay <slot>
/scoreboard objectives setdisplay <slot> <objective>
/scoreboard players add <targets> <objective> <score>
/scoreboard players display name <targets> <objective>
/scoreboard players display name <targets> <objective> <name>
/scoreboard players display numberformat <targets> <objective>
/scoreboard players display numberformat <targets> <objective> blank
/scoreboard players display numberformat <targets> <objective> fixed <contents>
/scoreboard players display numberformat <targets> <objective> styled <style>
/scoreboard players enable <targets> <objective>
/scoreboard players get <target> <objective>
/scoreboard players list
/scoreboard players list <target>
/scoreboard players operation <targets> <targetObjective> <operation> <source> <sourceObjective>
/scoreboard players remove <targets> <objective> <score>
/scoreboard players reset <targets>
/scoreboard players reset <targets> <objective>
/scoreboard players set <targets> <objective> <score>
```

### /searchitem

```text
/searchitem
/searchitem <args>
```

### /seed

```text
/seed
```

### /selwand

```text
/selwand
/selwand <args>
```

### /setblock

```text
/setblock <pos> <block>
/setblock <pos> <block> destroy
/setblock <pos> <block> keep
/setblock <pos> <block> replace
```

### /sethome

```text
/sethome
/sethome <nombre>
```

### /setidletimeout

```text
/setidletimeout <minutes>
```

### /setpwarp

```text
/setpwarp <nombre>
```

### /setworldspawn

```text
/setworldspawn
/setworldspawn <pos>
/setworldspawn <pos> <angle>
```

### /size

```text
/size
/size <args>
```

### /skin

```text
/skin clear
/skin clear <targets>
/skin config reload
/skin refresh
/skin reset
/skin reset <targets>
/skin set ely.by <username>
/skin set ely.by <username> <targets>
/skin set mojang <username>
/skin set mojang <username> <targets>
/skin set web classic <url>
/skin set web classic <url> <targets>
/skin set web slim <url>
/skin set web slim <url> <targets>
```

### /smithing

```text
/smithing
```

### /snap

```text
/snap
/snap <args>
```

### /snapshot

```text
/snapshot
/snapshot <args>
```

### /snow

```text
/snow
/snow <args>
```

### /sp

```text
/sp
/sp <args>
```

### /spark

```text
/spark
/spark <args>
```

### /spawnallpokemon

```text
/spawnallpokemon
/spawnallpokemon <min>
/spawnallpokemon <min> <max>
```

### /spawnnpc

```text
/spawnnpc <class>
/spawnnpc <class> <level>
```

### /spawnnpcat

```text
/spawnnpcat <pos> <class>
/spawnnpcat <pos> <class> <level>
```

### /spawnpoint

```text
/spawnpoint
/spawnpoint <targets>
/spawnpoint <targets> <pos>
/spawnpoint <targets> <pos> <angle>
```

### /spawnpokemon

```text
/spawnpokemon <properties>
```

### /spawnpokemonat

```text
/spawnpokemonat <pos> <properties>
```

### /spawnpokemonfrompool

```text
/spawnpokemonfrompool
/spawnpokemonfrompool <amount>
```

### /spawns

```text
/spawns
```

### /spectate

```text
/spectate
/spectate <target>
/spectate <target> <player>
```

### /spectateBattle

```text
/spectateBattle <player>
```

### /spreadplayers

```text
/spreadplayers <center> <spreadDistance> <maxRange> <respectTeams> <targets>
/spreadplayers <center> <spreadDistance> <maxRange> under <maxHeight> <respectTeams> <targets>
```

### /stonecutter

```text
/stonecutter
```

### /stop

```text
/stop
```

### /stopbattle

```text
/stopbattle <player>
```

### /stopsound

```text
/stopsound <targets>
/stopsound <targets> * <sound>
/stopsound <targets> ambient
/stopsound <targets> ambient <sound>
/stopsound <targets> block
/stopsound <targets> block <sound>
/stopsound <targets> hostile
/stopsound <targets> hostile <sound>
/stopsound <targets> master
/stopsound <targets> master <sound>
/stopsound <targets> music
/stopsound <targets> music <sound>
/stopsound <targets> neutral
/stopsound <targets> neutral <sound>
/stopsound <targets> player
/stopsound <targets> player <sound>
/stopsound <targets> record
/stopsound <targets> record <sound>
/stopsound <targets> voice
/stopsound <targets> voice <sound>
/stopsound <targets> weather
/stopsound <targets> weather <sound>
```

### /summon

```text
/summon <entity>
/summon <entity> <pos>
/summon <entity> <pos> <nbt>
```

### /superpickaxe

```text
/superpickaxe
/superpickaxe <args>
```

### /tag

```text
/tag <targets> add <name>
/tag <targets> list
/tag <targets> remove <name>
```

### /takemark

```text
/takemark <player> <slot> <mark>
```

### /takepokemon

```text
/takepokemon <player> <slot>
```

### /teach

```text
/teach <player> <slot> <move>
```

### /team

```text
/team add <team>
/team add <team> <displayName>
/team empty <team>
/team join <team>
/team join <team> <members>
/team leave <members>
/team list
/team list <team>
/team modify <team> collisionRule always
/team modify <team> collisionRule never
/team modify <team> collisionRule pushOtherTeams
/team modify <team> collisionRule pushOwnTeam
/team modify <team> color <value>
/team modify <team> deathMessageVisibility always
/team modify <team> deathMessageVisibility hideForOtherTeams
/team modify <team> deathMessageVisibility hideForOwnTeam
/team modify <team> deathMessageVisibility never
/team modify <team> displayName <displayName>
/team modify <team> friendlyFire <allowed>
/team modify <team> nametagVisibility always
/team modify <team> nametagVisibility hideForOtherTeams
/team modify <team> nametagVisibility hideForOwnTeam
/team modify <team> nametagVisibility never
/team modify <team> prefix <prefix>
/team modify <team> seeFriendlyInvisibles <allowed>
/team modify <team> suffix <suffix>
/team remove <team>
```

### /teammsg

```text
/teammsg <message>
```

### /tebex

```text
/tebex ban <playerName> <reason> <ip>
/tebex checkout <packageId>
/tebex debug <true/false>
/tebex forcecheck
/tebex goals
/tebex help
/tebex info
/tebex lookup <username>
/tebex reload
/tebex secret <key>
/tebex sendlink <packageId> <username>
```

### /technicalmachine

```text
/technicalmachine check <player>
/technicalmachine lock <player> all
/technicalmachine lock <player> only <TM>
/technicalmachine unlock <player> all
/technicalmachine unlock <player> only <TM>
```

### /teleport

```text
/teleport <destination>
/teleport <location>
/teleport <targets> <destination>
/teleport <targets> <location>
/teleport <targets> <location> <rotation>
/teleport <targets> <location> facing <facingLocation>
/teleport <targets> <location> facing entity <facingEntity>
/teleport <targets> <location> facing entity <facingEntity> <facingAnchor>
```

### /tell

```text
/tell tell -> msg
```

### /tellraw

```text
/tellraw <targets> <message>
```

### /testcommand

```text
/testcommand
```

### /testpartyslot

```text
/testpartyslot <player> <slot> <properties>
```

### /testpcslot

```text
/testpcslot <player> <box> <slot> <properties>
```

### /teststore

```text
/teststore <player> <store> <properties>
```

### /thaw

```text
/thaw
/thaw <args>
```

### /thru

```text
/thru
/thru <args>
```

### /tick

```text
/tick freeze
/tick query
/tick rate <rate>
/tick sprint <time>
/tick sprint stop
/tick step
/tick step <time>
/tick step stop
/tick unfreeze
```

### /time

```text
/time add <time>
/time query day
/time query daytime
/time query gametime
/time set <time>
/time set day
/time set midnight
/time set night
/time set noon
```

### /title

```text
/title <targets> actionbar <title>
/title <targets> clear
/title <targets> reset
/title <targets> subtitle <title>
/title <targets> times <fadeIn> <stay> <fadeOut>
/title <targets> title <title>
```

### /tm

```text
/tm tm -> teammsg
```

### /toggleeditwand

```text
/toggleeditwand
/toggleeditwand <args>
```

### /toggleplace

```text
/toggleplace
/toggleplace <args>
```

### /tool

```text
/tool
/tool <args>
```

### /tp

```text
/tp tp -> teleport
```

### /tpa

```text
/tpa <jugador>
```

### /tpaccept

```text
/tpaccept
```

### /tpcancel

```text
/tpcancel
```

### /tpdeny

```text
/tpdeny
```

### /tpno

```text
/tpno
```

### /tpyes

```text
/tpyes
```

### /tracemask

```text
/tracemask
/tracemask <args>
```

### /transfer

```text
/transfer <hostname>
/transfer <hostname> <port>
/transfer <hostname> <port> <players>
```

### /transformmodelpart

```text
/transformmodelpart <transform>
/transformmodelpart <transform> <part>
/transformmodelpart <transform> <part> <x> <y> <z>
```

### /trash

```text
/trash
```

### /tree

```text
/tree
/tree <args>
```

### /trigger

```text
/trigger <objective>
/trigger <objective> add <value>
/trigger <objective> set <value>
```

### /undo

```text
/undo
/undo <args>
```

### /unlockpcboxwallpaper

```text
/unlockpcboxwallpaper <player> <wallpaper>
/unlockpcboxwallpaper <player> <wallpaper> <playsSound>
```

### /unstuck

```text
/unstuck
/unstuck <args>
```

### /up

```text
/up
/up <args>
```

### /w

```text
/w w -> msg
```

### /we

```text
/we
/we <args>
```

### /weather

```text
/weather clear
/weather clear <duration>
/weather rain
/weather rain <duration>
/weather thunder
/weather thunder <duration>
```

### /whitelist

```text
/whitelist add <targets>
/whitelist list
/whitelist off
/whitelist on
/whitelist reload
/whitelist remove <targets>
```

### /workbench

```text
/workbench
```

### /worldborder

```text
/worldborder add <distance>
/worldborder add <distance> <time>
/worldborder center <pos>
/worldborder damage amount <damagePerBlock>
/worldborder damage buffer <distance>
/worldborder get
/worldborder set <distance>
/worldborder set <distance> <time>
/worldborder warning distance <distance>
/worldborder warning time <time>
```

### /worldedit

```text
/worldedit
/worldedit <args>
```

### /xp

```text
/xp xp -> experience
```
