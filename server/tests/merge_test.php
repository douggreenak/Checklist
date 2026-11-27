<?php
require __DIR__ . '/../merge.php';
function it($i,$t,$c,$o,$u){return ['i'=>$i,'t'=>$t,'tu'=>$u,'c'=>$c,'cu'=>$u,'o'=>$o,'ou'=>$u];}
function L($items,$extra=[]){return array_merge(['id'=>'L1','name'=>'A','showCompleted'=>false,'argb'=>1,'lu'=>5,'dead'=>0,'del'=>[],'items'=>$items],$extra);}
$ok=0;$bad=0; function t($n,$v){global $ok,$bad; if($v){$ok++;}else{$bad++;echo "FAIL $n\n";}}
$base=[L([it('a','milk',false,1,5),it('b','eggs',false,2,5)])];
// two people check different items
$s=[L([it('a','milk',true,1,10),it('b','eggs',false,2,5)])];
$c=[L([it('a','milk',false,1,5),it('b','eggs',true,2,12)])];
$m=cl_merge($s,$c)[0]['items'];
t('both checks kept',$m[0]['c']===true && $m[1]['c']===true);
// newer wins on same field
$s=[L([it('a','milk',true,1,20)])]; $c=[L([it('a','milk',false,1,10)])];
t('newer wins',cl_merge($s,$c)[0]['items'][0]['c']===true);
// delete stays deleted, and offline copy cannot resurrect
$s=[L([],['del'=>['a'=>30]])]; $c=[L([it('a','milk',false,1,5)])];
t('tombstone',count(cl_merge($s,$c)[0]['items'])===0);
// new items from both
$s=[L([it('a','x',false,1,5),it('s','new-s',false,3,6)])]; $c=[L([it('a','x',false,1,5),it('c','new-c',false,2,7)])];
$m=cl_merge($s,$c)[0]['items']; t('union ordered',array_column($m,'i')===['a','c','s']);
// list tombstone and new list
$s=[L([]),['id'=>'L2']+L([])]; $c=[L([],['dead'=>9])];
$m=cl_merge($s,$c); t('list dead',$m[0]['dead']===9 && count($m)===2);
// legacy doc gets ids, deterministic
$old=[['id'=>'Z','name'=>'n','items'=>[['t'=>'a','c'=>false],['t'=>'b','c'=>true]]]];
$n1=cl_normalize($old); $n2=cl_normalize($old);
t('migrate',$n1==$n2 && $n1[0]['items'][1]['c']===true && $n1[0]['items'][0]['i']!==$n1[0]['items'][1]['i']);
// idempotent
$x=cl_merge($base,$base); t('idempotent',cl_merge($x,$base)==$x);
echo "$ok ok, $bad failed\n";
