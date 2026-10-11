// Run against the engine extracted from the exact deployed Cobblemon JAR.
// node tools/tests/trainer_showdown_regression.cjs build/trainer-audit-181/showdown
const assert = require('node:assert/strict');
const path = require('node:path');
const {Battle} = require(path.resolve(process.argv[2], 'sim/battle.js'));
const {randomUUID} = require('node:crypto');
const results = [];
function pokemon(species, moves, level=50) {
  return {species, moves, level, uuid:randomUUID(),
    movesInfo:moves.map(() => ({pp:40,maxPp:40}))};
}
function fixture(team1, team2, doubles=false) {
  const output=[];
  const battle=new Battle({formatid:doubles?'gen9doublescustomgame':'gen9customgame',
    seed:[1,2,3,4], send:(type, data)=>output.push({type,data})});
  battle.setPlayer('p1',{name:'Challenger',team:team1});
  battle.setPlayer('p2',{name:'Trainer',team:team2});
  if (battle.requestState==='teampreview') {
    battle.choose('p1','team '+team1.map((_,i)=>i+1).join(''));
    battle.choose('p2','team '+team2.map((_,i)=>i+1).join(''));
  }
  battle.sendUpdates(); output.length=0;
  return {battle,output};
}
function test(name, fn) {fn();results.push({name,result:'PASS'});console.log('PASS',name);}
for (const side of ['p1','p2']) test(`KO replacement ${side}: no attack until next choice`,()=>{
  const weak=[pokemon('Magikarp',['splash'],1),pokemon('Blastoise',['tackle'])];
  const strong=[pokemon('Arcanine',['flamethrower'])];
  const {battle:b,output}=fixture(side==='p1'?weak:strong,side==='p1'?strong:weak);
  b.choose('p1','move 1');b.choose('p2','move 1');b.sendUpdates();
  const other=side==='p1'?'p2':'p1';
  assert.deepEqual(b[side].activeRequest.forceSwitch,[true]);
  assert.equal(b[other].activeRequest.wait,true);
  // Native engine emits requests before the faint/move update. Java must not
  // ask for a choice from RequestInstruction before the update has played.
  assert.ok(output.findIndex(o=>o.type==='sideupdate') < output.findIndex(o=>o.type==='update'));
  const from=b.log.length;
  b.choose(side,'switch 2');b.sendUpdates();
  assert.equal(b.requestState,'move');
  assert.equal(b.turn,2);
  assert.equal(b.log.slice(from).filter(l=>l.startsWith('|move|')).length,0);
  assert.equal(b[side].active[0].hp,b[side].active[0].maxhp);
  b.choose(other,'move 1');
  assert.equal(b.log.slice(from).filter(l=>l.startsWith('|move|')).length,0);
  b.choose(side,'move 1');
  assert.ok(b.log.slice(from).some(l=>l.startsWith('|move|')));
});
test('Voluntary switch spends an action and the opposing move still executes',()=>{
  const {battle:b}=fixture([pokemon('Blastoise',['tackle']),pokemon('Lapras',['surf'])],
    [pokemon('Arcanine',['flamethrower'])]);
  const from=b.log.length;b.choose('p1','switch 2');b.choose('p2','move 1');
  assert.equal(b.log.slice(from).filter(l=>l.startsWith('|move|p2')).length,1);
  assert.ok(b.p1.active[0].hp<b.p1.active[0].maxhp);
});
test('Fast U-turn replacement preserves the legitimate remaining enemy move',()=>{
  const {battle:b}=fixture([pokemon('Crobat',['uturn']),pokemon('Blastoise',['tackle'])],
    [pokemon('Snorlax',['tackle'])]);
  b.choose('p1','move 1');b.choose('p2','move 1');
  assert.deepEqual(b.p1.activeRequest.forceSwitch,[true]);
  const from=b.log.length;b.choose('p1','switch 2');
  assert.equal(b.log.slice(from).filter(l=>l.startsWith('|move|p2')).length,1);
});
test('Simultaneous KO requests two replacements, then a fresh move phase',()=>{
  const {battle:b}=fixture([pokemon('Electrode',['explosion'],100),pokemon('Blastoise',['tackle'])],
    [pokemon('Magikarp',['splash'],1),pokemon('Lapras',['surf'])]);
  b.choose('p1','move 1');b.choose('p2','move 1');
  assert.deepEqual(b.p1.activeRequest.forceSwitch,[true]);
  assert.deepEqual(b.p2.activeRequest.forceSwitch,[true]);
  const from=b.log.length;b.choose('p1','switch 2');b.choose('p2','switch 2');
  assert.equal(b.requestState,'move');
  assert.equal(b.log.slice(from).filter(l=>l.startsWith('|move|')).length,0);
});
test('A waiting side cannot attack during an opposing forced switch',()=>{
  const {battle:b,output}=fixture([pokemon('Magikarp',['splash'],1),pokemon('Blastoise',['tackle'])],
    [pokemon('Arcanine',['flamethrower'])]);
  b.choose('p1','move 1');b.choose('p2','move 1');
  const from=b.log.length;
  assert.equal(b.choose('p2','move 1'),false);
  assert.equal(b.log.length,from);
  assert.ok(output.some(o=>o.type==='sideupdate'&&String(o.data).includes('[Invalid choice]')));
});
test('Doubles: two KO slots with one bench use one switch and one legal pass',()=>{
  const {battle:b}=fixture([pokemon('Magikarp',['splash'],1),pokemon('Feebas',['splash'],1),
    pokemon('Blastoise',['tackle'])], [pokemon('Garchomp',['earthquake']),pokemon('Crobat',['splash'])],true);
  b.choose('p1','move 1, move 1');b.choose('p2','move 1, move 1');
  assert.deepEqual(b.p1.activeRequest.forceSwitch,[true,true]);
  const from=b.log.length;
  assert.equal(b.choose('p1','switch 3, pass'),true);
  assert.equal(b.requestState,'move');
  assert.equal(b.log.slice(from).filter(l=>l.startsWith('|move|')).length,0);
});
test('Doubles: two bench replacements cannot reserve the same Pokemon',()=>{
  const {battle:b}=fixture([pokemon('Magikarp',['splash'],1),pokemon('Feebas',['splash'],1),
    pokemon('Blastoise',['tackle']),pokemon('Lapras',['surf'])],
    [pokemon('Garchomp',['earthquake']),pokemon('Crobat',['splash'])],true);
  b.choose('p1','move 1, move 1');b.choose('p2','move 1, move 1');
  assert.equal(b.choose('p1','switch 3, switch 3'),false);
  assert.equal(b.choose('p1','switch 3, switch 4'),true);
  assert.equal(b.requestState,'move');
});
console.log(JSON.stringify({engine:path.resolve(process.argv[2]),tests:results},null,2));
