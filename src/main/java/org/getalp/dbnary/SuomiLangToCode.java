package org.getalp.dbnary;

import java.util.HashMap;

import org.getalp.blexisma.api.ISO639_3;

public class SuomiLangToCode {

static HashMap<String,String> suomiLangNameToLangCode = new HashMap<String,String>();
	
	static {
		suomiLangNameToLangCode.put("abhaasi","ab");
		suomiLangNameToLangCode.put("adyge","ady");
		suomiLangNameToLangCode.put("afar","aa");
		suomiLangNameToLangCode.put("afrihili","afh");
		suomiLangNameToLangCode.put("afrikaans","af");
		suomiLangNameToLangCode.put("aimara","ay");
		suomiLangNameToLangCode.put("ainu","ain");
		suomiLangNameToLangCode.put("akan","ak");
		suomiLangNameToLangCode.put("akkadi","akk");
		suomiLangNameToLangCode.put("alasaksa","nds");
		suomiLangNameToLangCode.put("alasorbi","dsb");
		suomiLangNameToLangCode.put("albania","sq");
		suomiLangNameToLangCode.put("aleutti","ale");
		suomiLangNameToLangCode.put("altai","alt");
		suomiLangNameToLangCode.put("amhara","am");
		suomiLangNameToLangCode.put("angika","anp");
		suomiLangNameToLangCode.put("arabia","ar");
		suomiLangNameToLangCode.put("aragonia","an");
		suomiLangNameToLangCode.put("aramea","arc");
		suomiLangNameToLangCode.put("arapho","arp");
		suomiLangNameToLangCode.put("araukaani","arn");
		suomiLangNameToLangCode.put("armenia","hy");
		suomiLangNameToLangCode.put("aromania","rup");
		suomiLangNameToLangCode.put("assami","as");
		suomiLangNameToLangCode.put("asturia","ast");
		suomiLangNameToLangCode.put("avaari","av");
		suomiLangNameToLangCode.put("avesta","ae");
		suomiLangNameToLangCode.put("azeri","az");
		suomiLangNameToLangCode.put("bali","ban");
		suomiLangNameToLangCode.put("bambara","bm");
		suomiLangNameToLangCode.put("basa","bas");
		suomiLangNameToLangCode.put("baski","eu");
		suomiLangNameToLangCode.put("baškiiri","ba");
		suomiLangNameToLangCode.put("bedža","bej");
		suomiLangNameToLangCode.put("bemba","bem");
		suomiLangNameToLangCode.put("bengali","bn");
		suomiLangNameToLangCode.put("bislama","bi");
		suomiLangNameToLangCode.put("blin","byn");
		suomiLangNameToLangCode.put("bosnia","bs");
		suomiLangNameToLangCode.put("bretoni","br");
		suomiLangNameToLangCode.put("bulgaria","bg");
		suomiLangNameToLangCode.put("burma","my");
		suomiLangNameToLangCode.put("caddo","cad");
		suomiLangNameToLangCode.put("cebuano","ceb");
		suomiLangNameToLangCode.put("cherokee","chr");
		suomiLangNameToLangCode.put("cheyenne","chy");
		suomiLangNameToLangCode.put("choctaw","cho");
		suomiLangNameToLangCode.put("chuuk","chk");
		suomiLangNameToLangCode.put("cree","cr");
		suomiLangNameToLangCode.put("creek","mus");
		suomiLangNameToLangCode.put("divehi","dv");
		suomiLangNameToLangCode.put("djula","dyu");
		suomiLangNameToLangCode.put("dogri","doi");
		suomiLangNameToLangCode.put("dzongkha","dz");
		suomiLangNameToLangCode.put("efik","efi");
		suomiLangNameToLangCode.put("englanti","en");
		suomiLangNameToLangCode.put("ersä","myv");
		suomiLangNameToLangCode.put("espanja","es");
		suomiLangNameToLangCode.put("esperanto","eo");
		suomiLangNameToLangCode.put("etelä-ndebele","nr");
		suomiLangNameToLangCode.put("eteläsaame","sma");
		suomiLangNameToLangCode.put("ewe","ee");
		suomiLangNameToLangCode.put("fääri","fo");
		suomiLangNameToLangCode.put("fidži","fj");
		suomiLangNameToLangCode.put("filipino","fil");
		suomiLangNameToLangCode.put("foinikia","phn");
		suomiLangNameToLangCode.put("friisi","fy");
		suomiLangNameToLangCode.put("friuli","fur");
		suomiLangNameToLangCode.put("fulani","ff");
		suomiLangNameToLangCode.put("gaeli","gd");
		suomiLangNameToLangCode.put("galego","gl");
		suomiLangNameToLangCode.put("ganda","lg");
		suomiLangNameToLangCode.put("gayo","gay");
		suomiLangNameToLangCode.put("gbaja","gba");
		suomiLangNameToLangCode.put("georgia","ka");
		suomiLangNameToLangCode.put("gondi","gon");
		suomiLangNameToLangCode.put("gootti","got");
		suomiLangNameToLangCode.put("gorontalo","gor");
		suomiLangNameToLangCode.put("grönlanti","kl");
		suomiLangNameToLangCode.put("guarani","gn");
		suomiLangNameToLangCode.put("gudžarati","gu");
		suomiLangNameToLangCode.put("gwich'in","gwi");
		suomiLangNameToLangCode.put("haitinkreoli","ht");
		suomiLangNameToLangCode.put("hausa","ha");
		suomiLangNameToLangCode.put("havaiji","haw");
		suomiLangNameToLangCode.put("heprea","he");
		suomiLangNameToLangCode.put("herero","hz");
		suomiLangNameToLangCode.put("hindi","hi");
		suomiLangNameToLangCode.put("hiri motu","ho");
		suomiLangNameToLangCode.put("hollanti","nl");
		suomiLangNameToLangCode.put("hupa","hup");
		suomiLangNameToLangCode.put("iban","iba");
		suomiLangNameToLangCode.put("ido","io");
		suomiLangNameToLangCode.put("igbo","ig");
		suomiLangNameToLangCode.put("iiri","ga");
		suomiLangNameToLangCode.put("iloko","ilo");
		suomiLangNameToLangCode.put("inarinsaame","smn");
		suomiLangNameToLangCode.put("indonesia","id");
		suomiLangNameToLangCode.put("inguuši","inh");
		suomiLangNameToLangCode.put("interlingua","ia");
		suomiLangNameToLangCode.put("interlingue","ie");
		suomiLangNameToLangCode.put("inuktitut","iu");
		suomiLangNameToLangCode.put("inupiatun","ik");
		suomiLangNameToLangCode.put("islanti","is");
		suomiLangNameToLangCode.put("italia","it");
		suomiLangNameToLangCode.put("jaava","jv");
		suomiLangNameToLangCode.put("jakuutti","sah");
		suomiLangNameToLangCode.put("japani","ja");
		suomiLangNameToLangCode.put("jiddiš","yi");
		suomiLangNameToLangCode.put("joruba","yo");
		suomiLangNameToLangCode.put("juutalaisarabia","jrb");
		suomiLangNameToLangCode.put("juutalaispersia","jpr");
		suomiLangNameToLangCode.put("kabardi","kbd");
		suomiLangNameToLangCode.put("kalmukki","xal");
		suomiLangNameToLangCode.put("kannada","kn");
		suomiLangNameToLangCode.put("kanuri","kr");
		suomiLangNameToLangCode.put("kapampangan","pam");
		suomiLangNameToLangCode.put("karakalpakki","kaa");
		suomiLangNameToLangCode.put("karatšai-balkaari","krc");
		suomiLangNameToLangCode.put("karibi","car");
		suomiLangNameToLangCode.put("karjala","krl");
		suomiLangNameToLangCode.put("kašmiri","ks");
		suomiLangNameToLangCode.put("kašubi","csb");
		suomiLangNameToLangCode.put("katalaani","ca");
		suomiLangNameToLangCode.put("kazakki","kk");
		suomiLangNameToLangCode.put("keskienglanti","enm");
		suomiLangNameToLangCode.put("keskihollanti","dum");
		suomiLangNameToLangCode.put("keskiranska","frm");
		suomiLangNameToLangCode.put("keskiyläsaksa","gmh");
		suomiLangNameToLangCode.put("ketšua","qu");
		suomiLangNameToLangCode.put("khmer","km");
		suomiLangNameToLangCode.put("kikuju","ki");
		suomiLangNameToLangCode.put("kinjaruanda","rw");
		suomiLangNameToLangCode.put("kirgiisi","ky");
		suomiLangNameToLangCode.put("kiribati","gil");
		suomiLangNameToLangCode.put("kirkkoslaavi","cu");
		suomiLangNameToLangCode.put("kirundi","rn");
		suomiLangNameToLangCode.put("klingon","tlh");
		suomiLangNameToLangCode.put("koltansaame","sms");
		suomiLangNameToLangCode.put("komi","kv");
		suomiLangNameToLangCode.put("kongo","kg");
		suomiLangNameToLangCode.put("kopti","cop");
		suomiLangNameToLangCode.put("korea","ko");
		suomiLangNameToLangCode.put("korni","kw");
		suomiLangNameToLangCode.put("korsika","co");
		suomiLangNameToLangCode.put("kosrae","kos");
		suomiLangNameToLangCode.put("kreikka","el");
		suomiLangNameToLangCode.put("krimintataari","crh");
		suomiLangNameToLangCode.put("kroatia","hr");
		suomiLangNameToLangCode.put("kurdi","ku");
		suomiLangNameToLangCode.put("kwanjama","kj");
		suomiLangNameToLangCode.put("kymri","cy");
		suomiLangNameToLangCode.put("ladino","lad");
		suomiLangNameToLangCode.put("lamba","lam");
		suomiLangNameToLangCode.put("lao","lo");
		suomiLangNameToLangCode.put("latina","la");
		suomiLangNameToLangCode.put("latvia","lv");
		suomiLangNameToLangCode.put("liettua","lt");
		suomiLangNameToLangCode.put("limburgi","li");
		suomiLangNameToLangCode.put("lingala","ln");
		suomiLangNameToLangCode.put("lojban","jbo");
		suomiLangNameToLangCode.put("lozi","loz");
		suomiLangNameToLangCode.put("luba","lu");
		suomiLangNameToLangCode.put("luba (Lulua)","lua");
		suomiLangNameToLangCode.put("luulajansaame","smj");
		suomiLangNameToLangCode.put("luxemburg","lb");
		suomiLangNameToLangCode.put("madura","mad");
		suomiLangNameToLangCode.put("magahi","mag");
		suomiLangNameToLangCode.put("makedonia","mk");
		suomiLangNameToLangCode.put("malagassi","mg");
		suomiLangNameToLangCode.put("malaiji","ms");
		suomiLangNameToLangCode.put("malajalam","ml");
		suomiLangNameToLangCode.put("malta","mt");
		suomiLangNameToLangCode.put("mandariinikiina","zh");
		suomiLangNameToLangCode.put("manipuri","mni");
		suomiLangNameToLangCode.put("manksi","gv");
		suomiLangNameToLangCode.put("mantšu","mnc");
		suomiLangNameToLangCode.put("maori","mi");
		suomiLangNameToLangCode.put("marathi","mr");
		suomiLangNameToLangCode.put("mari","chm");
		suomiLangNameToLangCode.put("marshall","mh");
		suomiLangNameToLangCode.put("mokša","mdf");
		suomiLangNameToLangCode.put("moldavia","mo");
		suomiLangNameToLangCode.put("mongoli","mn");
		suomiLangNameToLangCode.put("muinaisenglanti","ang");
		suomiLangNameToLangCode.put("muinaisiiri","sga");
		suomiLangNameToLangCode.put("muinaiskreikka","grc");
		suomiLangNameToLangCode.put("muinaisoksitaani","pro");
		suomiLangNameToLangCode.put("muinaisranska","fro");
		suomiLangNameToLangCode.put("muinaisyläsaksa","goh");
		suomiLangNameToLangCode.put("mustajalka","bla");
		suomiLangNameToLangCode.put("napoli","nap");
		suomiLangNameToLangCode.put("nauru","na");
		suomiLangNameToLangCode.put("navaho","nv");
		suomiLangNameToLangCode.put("ndonga","ng");
		suomiLangNameToLangCode.put("nepali","ne");
		suomiLangNameToLangCode.put("niue","niu");
		suomiLangNameToLangCode.put("njandža","ny");
		suomiLangNameToLangCode.put("n'ko","nqo");
		suomiLangNameToLangCode.put("norja (bokmål)","no");
		suomiLangNameToLangCode.put("norja (bokmål)","nb");
		suomiLangNameToLangCode.put("norja (nynorsk)","nn");
		suomiLangNameToLangCode.put("nuosu","ii");
		suomiLangNameToLangCode.put("nzima","nzi");
		suomiLangNameToLangCode.put("ojibwe","oj");
		suomiLangNameToLangCode.put("oksitaani","oc");
		suomiLangNameToLangCode.put("orija","or");
		suomiLangNameToLangCode.put("oromo","om");
		suomiLangNameToLangCode.put("osmani","ota");
		suomiLangNameToLangCode.put("osseetti","os");
		suomiLangNameToLangCode.put("paali","pi");
		suomiLangNameToLangCode.put("palau","pau");
		suomiLangNameToLangCode.put("paštu","ps");
		suomiLangNameToLangCode.put("persia","fa");
		suomiLangNameToLangCode.put("pohjois-ndebele","nd");
		suomiLangNameToLangCode.put("pohjoissaame","se");
		suomiLangNameToLangCode.put("pohjoissotho","nso");
		suomiLangNameToLangCode.put("ponape","pon");
		suomiLangNameToLangCode.put("portugali","pt");
		suomiLangNameToLangCode.put("punjabi","pa");
		suomiLangNameToLangCode.put("puola","pl");
		suomiLangNameToLangCode.put("ranska","fr");
		suomiLangNameToLangCode.put("rapanui","rap");
		suomiLangNameToLangCode.put("rarotonga","rar");
		suomiLangNameToLangCode.put("retoromaani","rm");
		suomiLangNameToLangCode.put("romani","rom");
		suomiLangNameToLangCode.put("romania","ro");
		suomiLangNameToLangCode.put("ruotsi","sv");
		suomiLangNameToLangCode.put("saka","kho");
		suomiLangNameToLangCode.put("saksa","de");
		suomiLangNameToLangCode.put("samoa","sm");
		suomiLangNameToLangCode.put("sango","sg");
		suomiLangNameToLangCode.put("sanskrit","sa");
		suomiLangNameToLangCode.put("santali","sat");
		suomiLangNameToLangCode.put("sardi","sc");
		suomiLangNameToLangCode.put("sasak","sas");
		suomiLangNameToLangCode.put("selkuppi","sel");
		suomiLangNameToLangCode.put("serbia","sr");
		suomiLangNameToLangCode.put("serbokroaatti","sh");
		suomiLangNameToLangCode.put("shona","sn");
		suomiLangNameToLangCode.put("sindhi","sd");
		suomiLangNameToLangCode.put("sinhali","si");
		suomiLangNameToLangCode.put("sisilia","scn");
		suomiLangNameToLangCode.put("skotti","sco");
		suomiLangNameToLangCode.put("slovakki","sk");
		suomiLangNameToLangCode.put("sloveeni","sl");
		suomiLangNameToLangCode.put("sogdi","sog");
		suomiLangNameToLangCode.put("somali","so");
		suomiLangNameToLangCode.put("soninke","snk");
		suomiLangNameToLangCode.put("sotho","st");
		suomiLangNameToLangCode.put("sumeri","sux");
		suomiLangNameToLangCode.put("sunda","su");
		suomiLangNameToLangCode.put("suomi","fi");
		suomiLangNameToLangCode.put("susu","sus");
		suomiLangNameToLangCode.put("swahili","sw");
		suomiLangNameToLangCode.put("swazi","ss");
		suomiLangNameToLangCode.put("tadžikki","tg");
		suomiLangNameToLangCode.put("tagalog","tl");
		suomiLangNameToLangCode.put("tahiti","ty");
		suomiLangNameToLangCode.put("tamili","ta");
		suomiLangNameToLangCode.put("tanska","da");
		suomiLangNameToLangCode.put("tataari","tt");
		suomiLangNameToLangCode.put("telugu","te");
		suomiLangNameToLangCode.put("temne","tem");
		suomiLangNameToLangCode.put("terêna","ter");
		suomiLangNameToLangCode.put("tetum","tet");
		suomiLangNameToLangCode.put("thai","th");
		suomiLangNameToLangCode.put("tigrinja","ti");
		suomiLangNameToLangCode.put("tiibet","bo");
		suomiLangNameToLangCode.put("tok pisin","tpi");
		suomiLangNameToLangCode.put("tokelau","tkl");
		suomiLangNameToLangCode.put("tonga","to");
		suomiLangNameToLangCode.put("tšagatai","chg");
		suomiLangNameToLangCode.put("tšamorro","ch");
		suomiLangNameToLangCode.put("tšekki","cs");
		suomiLangNameToLangCode.put("tšetšeeni","ce");
		suomiLangNameToLangCode.put("tsimsi","tsi");
		suomiLangNameToLangCode.put("tsonga","ts");
		suomiLangNameToLangCode.put("tšuvassi","cv");
		suomiLangNameToLangCode.put("tswana","tn");
		suomiLangNameToLangCode.put("tumbuka","tum");
		suomiLangNameToLangCode.put("turkki","tr");
		suomiLangNameToLangCode.put("turkmeeni","tk");
		suomiLangNameToLangCode.put("tuva","tyv");
		suomiLangNameToLangCode.put("tuvalu","tvl");
		suomiLangNameToLangCode.put("udmurtti","udm");
		suomiLangNameToLangCode.put("uiguuri","ug");
		suomiLangNameToLangCode.put("ukraina","uk");
		suomiLangNameToLangCode.put("unkari","hu");
		suomiLangNameToLangCode.put("urdu","ur");
		suomiLangNameToLangCode.put("uzbekki","uz");
		suomiLangNameToLangCode.put("valkovenäjä","be");
		suomiLangNameToLangCode.put("valloni","wa");
		suomiLangNameToLangCode.put("vatja","vot");
		suomiLangNameToLangCode.put("venäjä","ru");
		suomiLangNameToLangCode.put("venda","ve");
		suomiLangNameToLangCode.put("vietnam","vi");
		suomiLangNameToLangCode.put("viro","et");
		suomiLangNameToLangCode.put("volapük","vo");
		suomiLangNameToLangCode.put("waray","war");
		suomiLangNameToLangCode.put("wolof","wo");
		suomiLangNameToLangCode.put("xhosa","xh");
		suomiLangNameToLangCode.put("yap","yap");
		suomiLangNameToLangCode.put("yläsorbi","hsb");
		suomiLangNameToLangCode.put("zenaga","zen");
		suomiLangNameToLangCode.put("zhuang","za");
		suomiLangNameToLangCode.put("zulu","zu");
}
	
	public static String triletterCode(String s){ 
		if(s!=null && s!="") {
		s= s.trim();
		s=s.toLowerCase();
	    String resultat;
		if (ISO639_3.sharedInstance.getIdCode(s) != null) {
			resultat =ISO639_3.sharedInstance.getIdCode(s);
		}else{
			if(suomiLangNameToLangCode.containsKey(s)){
				s=suomiLangNameToLangCode.get(s);
				
					resultat =ISO639_3.sharedInstance.getIdCode(s);
				
			}else {
				resultat=null;
			}
			
		}
		return resultat;
	}else{
	
		return s;
	}
	}
}
