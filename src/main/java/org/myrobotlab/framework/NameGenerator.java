package org.myrobotlab.lang;

import java.util.Random;

import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class NameGenerator {

  public final static Logger log = LoggerFactory.getLogger(NameGenerator.class);

  static String[] adj = new String[] { "aback", "abaft", "abandoned", "abashed", "aberrant", "abhorrent", "abiding", "abject", "ablaze", "able", "abnormal", "aboard", "aboriginal",
      "abortive", "abounding", "abrasive", "abrupt", "absent", "absorbed", "absorbing", "abstracted", "absurd", "abundant", "abusive", "acceptable", "accessible", "accidental",
      "accurate", "acid", "acidic", "acoustic", "acrid", "actually", "ad-hoc", "adamant", "adaptable", "addicted", "adhesive", "adjoining", "adorable", "adventurous", "afraid",
      "aggressive", "agonizing", "agreeable", "ahead", "ajar", "alcoholic", "alert", "alike", "alive", "alleged", "alluring", "aloof", "amazing", "ambiguous", "ambitious", "amuck",
      "amused", "amusing", "ancient", "angry", "animated", "annoyed", "annoying", "anxious", "apathetic", "aquatic", "aromatic", "arrogant", "ashamed", "aspiring", "assorted",
      "astonishing", "attractive", "auspicious", "automatic", "available", "average", "awake", "aware", "awesome", "awful", "axiomatic", "bad", "barbarous", "bashful", "bawdy",
      "beautiful", "befitting", "belligerent", "beneficial", "bent", "berserk", "best", "better", "bewildered", "big", "billowy", "bitter", "bizarre", "black", "bloody", "blue",
      "blushing", "boiling", "boorish", "bored", "boring", "bouncy", "boundless", "brainy", "brash", "brave", "brawny", "breakable", "breezy", "brief", "bright", "bright", "broad",
      "broken", "brown", "bumpy", "burly", "bustling", "busy", "cagey", "calculating", "callous", "calm", "capable", "capricious", "careful", "careless", "caring", "cautious",
      "ceaseless", "certain", "changeable", "charming", "cheap", "cheerful", "chemical", "chief", "childlike", "chilly", "chivalrous", "chubby", "chunky", "clammy", "classy",
      "clean", "clear", "clever", "cloistered", "cloudy", "closed", "clumsy", "cluttered", "coherent", "cold", "colorful", "colossal", "combative", "comfortable", "common",
      "complete", "complex", "concerned", "condemned", "confused", "conscious", "cooing", "cool", "cooperative", "coordinated", "courageous", "cowardly", "crabby", "craven",
      "crazy", "creepy", "crooked", "crowded", "cruel", "cuddly", "cultured", "cumbersome", "curious", "curly", "curved", "curvy", "cut", "cute", "cute", "cynical", "daffy",
      "daily", "damaged", "damaging", "damp", "dangerous", "dapper", "dark", "dashing", "dazzling", "dead", "deadpan", "deafening", "dear", "debonair", "decisive", "decorous",
      "deep", "deeply", "defeated", "defective", "defiant", "delicate", "delicious", "delightful", "demonic", "delirious", "dependent", "depressed", "deranged", "descriptive",
      "deserted", "detailed", "determined", "devilish", "didactic", "different", "difficult", "diligent", "direful", "dirty", "disagreeable", "disastrous", "discreet", "disgusted",
      "disgusting", "disillusioned", "dispensable", "distinct", "disturbed", "divergent", "dizzy", "domineering", "doubtful", "drab", "draconian", "dramatic", "dreary", "drunk",
      "dry", "dull", "dusty", "dusty", "dynamic", "dysfunctional", "eager", "early", "earsplitting", "earthy", "easy", "eatable", "economic", "educated", "efficacious",
      "efficient", "eight", "elastic", "elated", "elderly", "electric", "elegant", "elfin", "elite", "embarrassed", "eminent", "empty", "enchanted", "enchanting", "encouraging",
      "endurable", "energetic", "enormous", "entertaining", "enthusiastic", "envious", "equable", "equal", "erect", "erratic", "ethereal", "evanescent", "evasive", "even",
      "excellent", "excited", "exciting", "exclusive", "exotic", "expensive", "extra", "exuberant", "exultant", "fabulous", "faded", "faint", "fair", "faithful", "fallacious",
      "false", "familiar", "famous", "fanatical", "fancy", "fantastic", "far", "fascinated", "fast", "fat", "faulty", "fearful", "fearless", "feeble", "feigned", "female",
      "fertile", "festive", "few", "fierce", "filthy", "fine", "finicky", "first", "five", "fixed", "flagrant", "flaky", "flashy", "flat", "flawless", "flimsy", "flippant",
      "flowery", "fluffy", "fluttering", "foamy", "foolish", "foregoing", "forgetful", "fortunate", "four", "frail", "fragile", "frantic", "free", "freezing", "frequent", "fresh",
      "fretful", "friendly", "frightened", "frightening", "full", "fumbling", "functional", "funny", "furry", "furtive", "future", "futuristic", "fuzzy", "gabby", "gainful",
      "gamy", "gaping", "garrulous", "gaudy", "general", "gentle", "giant", "giddy", "gifted", "gigantic", "glamorous", "gleaming", "glib", "glistening", "glorious", "glossy",
      "godly", "good", "goofy", "gorgeous", "graceful", "grandiose", "grateful", "gratis", "gray", "greasy", "great", "greedy", "green", "grey", "grieving", "groovy", "grotesque",
      "grouchy", "grubby", "gruesome", "grumpy", "guarded", "guiltless", "gullible", "gusty", "guttural", "habitual", "half", "hallowed", "halting", "handsome", "handsomely",
      "handy", "hanging", "hapless", "happy", "hard", "harmonious", "harsh", "hateful", "heady", "healthy", "heartbreaking", "heavenly", "heavy", "hellish", "helpful", "helpless",
      "hesitant", "hideous", "high", "highfalutin", "hilarious", "hissing", "historical", "holistic", "hollow", "homeless", "homely", "honorable", "horrible", "hospitable", "hot",
      "huge", "hulking", "humdrum", "humorous", "hungry", "hurried", "hurt", "hushed", "husky", "hypnotic", "hysterical", "icky", "icy", "idiotic", "ignorant", "ill", "illegal",
      "illustrious", "imaginary", "immense", "imminent", "impartial", "imperfect", "impolite", "important", "imported", "impossible", "incandescent", "incompetent", "inconclusive",
      "industrious", "incredible", "inexpensive", "infamous", "innate", "innocent", "inquisitive", "insidious", "instinctive", "intelligent", "interesting", "internal",
      "invincible", "irate", "irritating", "itchy", "jaded", "jagged", "jazzy", "jealous", "jittery", "jobless", "jolly", "joyous", "judicious", "juicy", "jumbled", "jumpy",
      "juvenile", "kaput", "keen", "kind", "kindhearted", "kindly", "knotty", "knowing", "knowledgeable", "known", "labored", "lackadaisical", "lacking", "lame", "lamentable",
      "languid", "large", "last", "late", "laughable", "lavish", "lazy", "lean", "learned", "left", "legal", "lethal", "level", "lewd", "light", "like", "likeable", "limping",
      "literate", "little", "lively", "lively", "living", "lonely", "long", "longing", "loose", "lopsided", "loud", "loutish", "lovely", "loving", "low", "lowly", "lucky",
      "ludicrous", "lumpy", "lush", "luxuriant", "lying", "lyrical", "macabre", "macho", "maddening", "madly", "magenta", "magical", "magnificent", "majestic", "makeshift", "male",
      "malicious", "mammoth", "maniacal", "many", "marked", "massive", "married", "marvelous", "material", "materialistic", "mature", "mean", "measly", "meaty", "medical", "meek",
      "mellow", "melodic", "melted", "merciful", "mere", "messy", "mighty", "military", "milky", "mindless", "miniature", "minor", "miscreant", "misty", "mixed", "moaning",
      "modern", "moldy", "momentous", "motionless", "mountainous", "muddled", "mundane", "murky", "mushy", "mute", "mysterious", "naive", "nappy", "narrow", "nasty", "natural",
      "naughty", "nauseating", "near", "neat", "nebulous", "necessary", "needless", "needy", "neighborly", "nervous", "new", "next", "nice", "nifty", "nimble", "nine", "nippy",
      "noiseless", "noisy", "nonchalant", "nondescript", "nonstop", "normal", "nostalgic", "nosy", "noxious", "null", "numberless", "numerous", "nutritious", "nutty", "oafish",
      "obedient", "obeisant", "obese", "obnoxious", "obscene", "obsequious", "observant", "obsolete", "obtainable", "oceanic", "odd", "offbeat", "old", "omniscient", "one",
      "onerous", "open", "opposite", "optimal", "orange", "ordinary", "organic", "ossified", "outgoing", "outrageous", "outstanding", "oval", "overconfident", "overjoyed",
      "overrated", "overt", "overwrought", "painful", "painstaking", "pale", "paltry", "panicky", "panoramic", "parallel", "parched", "parsimonious", "past", "pastoral",
      "pathetic", "peaceful", "penitent", "perfect", "periodic", "permissible", "perpetual", "petite", "petite", "phobic", "physical", "picayune", "pink", "piquant", "placid",
      "plain", "plant", "plastic", "plausible", "pleasant", "plucky", "pointless", "poised", "polite", "political", "poor", "possessive", "possible", "powerful", "precious",
      "premium", "present", "pretty", "previous", "pricey", "prickly", "private", "probable", "productive", "profuse", "protective", "proud", "psychedelic", "psychotic", "public",
      "puffy", "pumped", "puny", "purple", "purring", "pushy", "puzzled", "puzzling", "quack", "quaint", "quarrelsome", "questionable", "quick", "quickest", "quiet", "quirky",
      "quixotic", "quizzical", "rabid", "racial", "ragged", "rainy", "rambunctious", "rampant", "rapid", "rare", "raspy", "ratty", "ready", "real", "rebel", "receptive",
      "recondite", "red", "redundant", "reflective", "regular", "relieved", "remarkable", "reminiscent", "repulsive", "resolute", "resonant", "responsible", "rhetorical", "rich",
      "right", "righteous", "rightful", "rigid", "ripe", "ritzy", "roasted", "robust", "romantic", "roomy", "rotten", "rough", "round", "royal", "ruddy", "rude", "rural", "rustic",
      "ruthless", "sable", "sad", "safe", "salty", "same", "sassy", "satisfying", "savory", "scandalous", "scarce", "scared", "scary", "scattered", "scientific", "scintillating",
      "scrawny", "screeching", "second", "secret", "secretive", "sedate", "seemly", "selective", "selfish", "separate", "serious", "shaggy", "shaky", "shallow", "sharp", "shiny",
      "shivering", "shocking", "short", "shrill", "shut", "shy", "sick", "silent", "silent", "silky", "silly", "simple", "simplistic", "sincere", "six", "skillful", "skinny",
      "sleepy", "slim", "slimy", "slippery", "sloppy", "slow", "small", "smart", "smelly", "smiling", "smoggy", "smooth", "sneaky", "snobbish", "snotty", "soft", "soggy", "solid",
      "somber", "sophisticated", "sordid", "sore", "sore", "sour", "sparkling", "special", "spectacular", "spicy", "spiffy", "spiky", "spiritual", "spiteful", "splendid", "spooky",
      "spotless", "spotted", "spotty", "spurious", "squalid", "square", "squealing", "squeamish", "staking", "stale", "standing", "statuesque", "steadfast", "steady", "steep",
      "stereotyped", "sticky", "stiff", "stimulating", "stingy", "stormy", "straight", "strange", "striped", "strong", "stupendous", "stupid", "sturdy", "subdued", "subsequent",
      "substantial", "successful", "succinct", "sudden", "sulky", "super", "superb", "superficial", "supreme", "swanky", "sweet", "sweltering", "swift", "symptomatic",
      "synonymous", "taboo", "tacit", "tacky", "talented", "tall", "tame", "tan", "tangible", "tangy", "tart", "tasteful", "tasteless", "tasty", "tawdry", "tearful", "tedious",
      "teeny", "telling", "temporary", "ten", "tender", "tense", "tense", "tenuous", "terrible", "terrific", "tested", "testy", "thankful", "therapeutic", "thick", "thin",
      "thinkable", "third", "thirsty", "thirsty", "thoughtful", "thoughtless", "threatening", "three", "thundering", "tidy", "tight", "tightfisted", "tiny", "tired", "tiresome",
      "toothsome", "torpid", "tough", "towering", "tranquil", "trashy", "tremendous", "tricky", "trite", "troubled", "truculent", "true", "truthful", "two", "typical",
      "ubiquitous", "ugliest", "ugly", "ultra", "unable", "unaccountable", "unadvised", "unarmed", "unbecoming", "unbiased", "uncovered", "understood", "undesirable", "unequal",
      "unequaled", "uneven", "unhealthy", "uninterested", "unique", "unkempt", "unknown", "unnatural", "unruly", "unsightly", "unsuitable", "untidy", "unused", "unusual",
      "unwieldy", "unwritten", "upbeat", "uppity", "upset", "uptight", "used", "useful", "useless", "utopian", "utter", "uttermost", "vacuous", "vagabond", "vague", "valuable",
      "various", "vast", "vengeful", "venomous", "verdant", "versed", "victorious", "vigorous", "violent", "violet", "vivacious", "voiceless", "volatile", "voracious", "vulgar",
      "wacky", "waggish", "waiting", "wakeful", "wandering", "wanting", "warlike", "warm", "wary", "wasteful", "watery", "weak", "wealthy", "weary", "wet", "whimsical",
      "whispering", "white", "whole", "wholesale", "wicked", "wide", "wiggly", "wild", "willing", "windy", "wiry", "wise", "wistful", "witty", "woebegone", "womanly", "wonderful",
      "wooden", "woozy", "workable", "worried", "worthless", "wrathful", "wretched", "wrong", "wry", "xenophobic", "yellow", "yielding", "young", "youthful", "yummy", "zany",
      "zealous", "zesty", "zippy", "zonked" };

  static String[] nouns = new String[] { "wozniak", "jobs", "gates", "turing", "einstein", "zuckerberg", "c3po", "r2d2", "hal", "computer", "robocop", "optimusprime", "terminator",
      "bender", "t1000", "hal9000", "walle", "worke", "motherboard", "wire", "astroboy", "megatron", "mecagodizilla", "calculon", "johnny5", "marvin", "megaman", "tron", "cyclons",
      "haro", "peepo", "golem", "hadaly", "android", "robot", "tiktok", "metropolis", "autonoma", "chappie", "bicentennialman", "irobot", "exmachina", "westworld", "automata",
      "daryl", "machine", "eva", "morgan", "tau", "axl", "caleb", "ava", "nathan", "kyoko", "rex", "asimov", "isaac", "bors", "droids", "drone", "chip", "microchip", "arduino",
      "jetsons", "autobots", "decepticons", "yod", "otis", "mrroboto", "futurama", "gort", "robby", "colossus", "omega", "cyborg", "huey", "dewey", "louie", "stepford", "logan",
      "zeroid", "galaxina", "hector", "saturn3", "killbots", "bishop", "jinx", "data", "ulysses", "schwarzenegger", "arnold", "weebo", "robogadget", "gadget", "spyder", "precrime",
      "zathura", "transmorphers", "giddy", "eva", "gort", "gerty", "brainbots", "mecwilly", "exmachina", "andromeda", "rosie", "drwho", "astroboy", "daleks", "fembots", "darth",
      "vader", "gundam", "haro", "futurepast", "longnow", "blakes7", "cybermen", "terrahawks", "kiljoy", "zeroid", "roboz", "buzzwang", "tobor" };

  static public String getName() {
    Random random = new Random(System.currentTimeMillis());
    StringBuilder sb = new StringBuilder();
    sb.append(adj[random.nextInt(adj.length)]);
    sb.append("-");
    sb.append(nouns[random.nextInt(nouns.length)]);

    return sb.toString();
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);
      for (int i = 0; i < 100; i++) {
        log.info(NameGenerator.getName());
      }

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

}
