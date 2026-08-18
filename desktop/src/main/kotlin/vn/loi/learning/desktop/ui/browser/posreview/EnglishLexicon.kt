package vn.loi.learning.desktop.ui.browser.posreview

import java.util.Locale

/**
 * Offline, deterministic lexical database for English vocabulary classification.
 * Covers everyday English, Oxford Picture Dictionary categories, CEFR A1-C1 vocabulary.
 */
object EnglishLexicon {

    enum class LexicalCategory {
        NOUN,
        VERB,
        ADJECTIVE,
        ADVERB,
        PREPOSITION,
        PRONOUN,
        CONJUNCTION,
        INTERJECTION,
        NUMBER,
        DETERMINER,
        ARTICLE,
        AUXILIARY,
        MODAL
    }

    // Proper Nouns / Calendar / Time
    private val properNounsAndCalendar = setOf(
        "january", "february", "march", "april", "may", "june",
        "july", "august", "september", "october", "november", "december",
        "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday",
        "spring", "summer", "autumn", "fall", "winter",
        "christmas", "easter", "halloween", "thanksgiving", "new year", "valentine",
        "earth", "moon", "sun", "asia", "europe", "africa", "america", "australia", "antarctica",
        "english", "vietnamese", "spanish", "french", "chinese", "japanese", "korean", "german"
    )

    // Numbers
    private val numberWords = setOf(
        "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
        "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen",
        "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety",
        "hundred", "thousand", "million", "billion", "trillion", "first", "second", "third",
        "fourth", "fifth", "sixth", "seventh", "eighth", "ninth", "tenth", "half", "quarter",
        "dozen", "score", "percent", "percentage"
    )

    // Pronouns
    private val pronounWords = setOf(
        "i", "you", "he", "she", "it", "we", "they", "me", "him", "her", "us", "them",
        "myself", "yourself", "himself", "herself", "itself", "ourselves", "themselves",
        "someone", "somebody", "something", "anyone", "anybody", "anything",
        "everyone", "everybody", "everything", "no one", "nobody", "nothing",
        "who", "whom", "whose", "which", "what", "this", "that", "these", "those"
    )

    // Prepositions
    private val prepositionWords = setOf(
        "about", "above", "across", "after", "against", "along", "among", "around", "at",
        "before", "behind", "below", "beneath", "beside", "between", "beyond", "by",
        "down", "during", "except", "for", "from", "in", "inside", "into", "near", "of",
        "off", "on", "onto", "opposite", "out", "outside", "over", "past", "round", "since",
        "through", "throughout", "to", "toward", "towards", "under", "underneath", "until",
        "unto", "up", "upon", "with", "within", "without"
    )

    // Conjunctions
    private val conjunctionWords = setOf(
        "and", "but", "or", "nor", "for", "yet", "so", "although", "because", "since",
        "unless", "while", "whereas", "if", "whether", "as", "though", "even if", "even though",
        "so that", "in order that", "provided that", "as soon as", "as long as"
    )

    // Interjections
    private val interjectionWords = setOf(
        "hello", "hi", "hey", "bye", "goodbye", "oh", "ah", "wow", "oops", "ouch",
        "yay", "hurray", "alas", "bravo", "bingo", "gosh", "gee", "phew", "shh",
        "welcome", "congratulations", "cheers", "please", "thanks", "thank you", "yes", "no", "okay", "ok"
    )

    // Common Nouns (Curated comprehensive collection covering OPD_2nd, everyday items, places, occupations, anatomy)
    private val commonNouns = setOf(
        // Clothing & Accessories
        "apron", "bathrobe", "belt", "blouse", "boots", "bow", "bow tie", "bra", "bracelet", "cap", "cardigan", "coat", "collar", "cuff", "dress",
        "earring", "earrings", "flip-flops", "glasses", "glove", "gloves", "handbag", "hat", "headband", "heels", "hood", "hoodie", "jacket", "jeans",
        "mitten", "mittens", "necklace", "nightgown", "overalls", "pajamas", "panties", "pants", "pocket", "purse", "raincoat", "ring",
        "robe", "sandal", "sandals", "scarf", "shirt", "shoe", "shoes", "shorts", "skirt",
        "sleeve", "slippers", "sneaker", "sneakers", "socks", "stockings", "suit", "sunglasses", "sunhat", "suspenders", "sweater", "sweatshirt",
        "swimsuit", "swimwear", "tank top", "tie", "tights", "trousers", "t-shirt", "tuxedo", "umbrella", "underpants", "undershirt",
        "underwear", "uniform", "veil", "vest", "wallet", "watch", "zipper",
        "dust ruffle", "hairnet", "name tag", "sweatpants", "earmuffs", "leggings", "swimming trunks", "windbreaker",
        "flats", "loafers", "locket", "oxfords", "scarves", "shoelaces", "string of pearls", "strand of beads", "beads",

        // Anatomy / Body
        "abdomen", "ankle", "anus", "appendix", "arm", "armpit", "artery", "back", "beard", "belly", "blood", "bone", "brain", "breast", "buttocks",
        "calf", "cheek", "chest", "chin", "colon", "ear", "earlobe", "elbow", "eye", "eyebrow", "eyelash", "eyelashes",
        "eyelid", "face", "finger", "fingernail", "foot", "feet", "forearm", "forehead", "gallbladder", "gland", "groin", "gum", "gums",
        "hair", "hand", "head", "heel", "hip", "iris", "jaw", "joint", "kidney", "knee", "knuckle", "leg", "ligament", "lip",
        "lips", "liver", "lung", "lungs", "moustache", "mustache", "mouth", "muscle", "nail",
        "navel", "neck", "nipple", "nose", "nostril", "palm", "pancreas", "pelvis", "pupil", "rib", "ribs", "shin", "shoulder",
        "sideburns", "skeleton", "skin", "skull", "spine", "spleen", "stomach", "teeth", "temple", "tendon", "thigh",
        "throat", "thumb", "toe", "toenail", "tongue", "tonsils", "tooth", "torso", "uterus", "vein", "waist", "wrist",
        "eardrum", "esophagus", "flesh", "freckles", "genitals", "humerus", "instep", "intestines", "kneecap", "large intestine",
        "limb", "mucus", "organ", "ovary", "penis", "phlegm", "rectum", "rib cage", "shoulder blade", "small intestine",
        "spinal column", "testicles", "thumbnail", "urine", "vagina", "vertebra", "vomit", "windpipe", "wrinkles",

        // Home, Furniture & Kitchen
        "air conditioner", "aisle", "alarm clock", "alley", "armchair", "ashtray", "attic", "auditorium", "balcony", "basement", "basin",
        "bag", "bags", "basket", "bath", "bathroom", "bathtub", "bed", "bedroom", "bedspread", "bellows", "binder", "blanket",
        "bleach", "blender", "boiler", "bookcase", "booth", "bottle", "bowl", "box", "boxes", "broom", "bucket", "buffer", "buzzer", "cabinet", "caliper", "can opener",
        "candle", "canteen", "carafe", "carpet", "casserole", "cauldron", "ceiling", "cellar", "centrifuge", "chair", "chimney", "chisel", "clamp", "cleaver", "clipper", "clippers", "clock", "closet", "coaster",
        "coffee maker", "colander", "comb", "compass", "cork", "corkscrew", "corridor", "cot", "couch", "counter", "crate", "crib", "cruet", "cubicle", "cup", "cupboard", "curtain",
        "curtains", "cushion", "decanter", "desk", "diaper", "diffuser", "dining room", "dishwasher", "dispenser", "doll", "door", "doorbell", "doorknob", "doormat", "dowel", "drain", "drainer",
        "drawer", "dredge", "dresser", "drill", "dropper", "dryer", "dustpan", "duvet", "easel", "fan", "faucet", "fence", "filter", "fire extinguisher", "fireplace", "flask", "fleece",
        "floor", "foil", "fork", "freezer", "fridge", "frying pan", "funnel", "furnace", "furniture", "fuse", "futon", "garage",
        "garbage can", "garden", "garlic press", "gasket", "gauge", "generator", "glass", "goblet", "gong", "goods", "grater", "griddle", "grill", "grinder", "hall", "hallway", "hammock", "hanger", "harness", "hatchet", "heater",
        "helmet", "hinge", "hoe", "hose", "hydrant", "incubator", "iron", "ironing board", "jack", "jar", "keg", "kettle", "kiln", "kitchen", "knife", "knives", "ladder", "ladle",
        "lamp", "lampshade", "lantern", "latch", "laundry", "lawn", "lawnmower", "level", "lever", "light", "living room", "lock", "locker", "loom", "lotion",
        "magnifier", "mallet", "manhole", "mannequin", "mantel", "mat", "match", "matches", "mattress", "meter", "microwave", "mill", "mirror", "mixer", "mop", "mortar", "mould", "mower", "mug", "napkin", "needle",
        "nightstand", "nozzle", "oar", "opener", "oven", "padlock", "pail", "pan", "pantograph", "pantry", "paper clip", "peeler", "pellet", "pestle", "piano", "picture", "pillow", "pillowcase", "pipe", "pitcher", "pitchfork",
        "planer", "plank", "plate", "pliers", "plow", "plunger", "porch", "pot", "potter", "pouch", "press", "pulley", "pump", "quilt", "rack", "radiator", "rake", "ramp", "rasper", "rattle", "reamer", "reel", "refrigerator", "respirator", "retort", "rivet", "rod", "roller", "roof", "room", "rope", "router",
        "rug", "sack", "safe", "safety pin", "sandpaper", "sash", "saucepan", "saucer", "saw", "scaffold", "scale", "scalpel", "scissors", "scoop", "scourer", "scouring pad", "scraper", "screen", "screw", "screwdriver", "scythe", "seeder", "sentry", "septic", "shackle", "shaker", "shears", "shed", "sheet", "shelf",
        "shelves", "shield", "shingle", "shovel", "shower", "shower curtain", "shredder", "shutter", "shuttle", "sieve", "sifter", "sink", "siphon", "siren", "skillet", "sled", "sledgehammer", "slicer", "smoker", "snorkel", "soap", "socket", "sofa", "spade", "spanner", "spatula", "speaker", "spear", "spectacles", "spigot", "spindle", "sponge", "spool", "spoon", "sprayer", "spring", "sprinkler", "squeegee",
        "stairs", "stake", "stand", "staple", "stapler", "steamer", "stencil", "step", "steps", "stethoscope", "stilts", "stoker", "stool", "stopper", "stove", "strainer", "strap", "stretcher", "strut", "stud", "subwoofer", "suction", "sump", "swab", "switch", "syringe", "table", "tablecloth", "tack", "tank", "tap", "tarp", "teapot", "telescope", "tether", "thimble", "timer",
        "television", "thermostat", "toaster", "toilet", "toilet paper", "tongs", "tool", "toolbox", "torch", "towel", "towel rack", "trailer", "trampoline", "transformer", "trap", "trash", "trash can", "tray", "treadmill", "trowel", "tub", "tube", "turbine", "tweezers", "tv", "typewriter", "umbrella", "urn", "utensil", "vacuum", "vacuum cleaner", "valve", "vane", "vase", "vault", "ventilator", "vise", "waffle maker", "wagon", "wall", "wardrobe",
        "wash", "washcloth", "washer", "washing machine", "wastebasket", "wedge", "wheel", "wheelbarrow", "wheelchair", "whistle", "wick", "winch", "window", "windowsill", "wipes", "wire", "wok", "workbench", "wrench", "wringer", "yoke",
        "bedbugs", "broiler", "candle holder", "cell phone holder", "chilli paste", "circuit breaker", "compost pile", "cotton bud", "cutting board", "deck", "dollhouse", "door chain", "drapes", "drill bit", "drill chuck", "driveway", "electric pencil sharpener", "electric shaver", "elevator", "emergency exit", "emery board", "escalator", "exterminator", "eyeliner", "eye shadow", "fabric softener", "farm", "feather duster", "flashlight", "furniture polish", "garbage disposal", "gate", "grease", "gutter", "hacksaw", "hair gel", "hamper", "headboard", "houseplant", "ink cartridge", "light bulb", "light fixture", "lipstick", "locksmith", "magazine holder", "mailer", "mascara", "mini-blinds", "mop refill", "mousetrap", "mouthwash", "nail polish", "nipper", "patio", "pegboard", "photocopier", "picture frame", "placemat", "plastic storage container", "platter", "playpen", "pot holders", "power sander", "rags", "ranch", "razor blade", "recliner", "recycling bin", "repair person", "roofer", "rubber band", "satellite dish", "scrub brush", "security gate", "shade", "shower gel", "showerhead", "silverware", "smoke alarm", "smoke detector", "soap dish", "staple remover", "staircase", "stepladder", "stroller", "sunblock", "switchboard", "switchboard operator", "teacup", "teakettle", "tenant", "toilet brush", "toothbrush holder", "toothpick", "trash bin", "trash chute", "walker", "wallpaper", "watering can", "water softener", "webcam",
        // Food, Ingredients, Spices & Meals
        "almond", "almonds", "anise", "appetizer", "apple", "apricot", "artichoke", "asparagus", "avocado", "bacon", "bagel", "baguette",
        "bamboo shoot", "banana", "barbecue", "barley", "basil", "bean", "beans", "bean sprout", "bean sprouts", "beef", "beer", "beet",
        "beets", "biscuit", "biscuits", "blackberry", "blackberries", "blueberry", "blueberries", "bok choy",
        "bread", "breakfast", "broccoli", "broth", "buckle", "bun", "buns", "butter", "cabbage", "cake",
        "candy", "cantaloupe", "caper", "capers", "cardamom", "carrot", "carrots", "cashew", "cashews",
        "cauliflower", "cayenne", "celery", "cereal", "cheese", "cherry", "cherries", "chia", "chicken", "chili",
        "chips", "chocolate", "chop", "cider", "cilantro", "cinnamon", "clam", "clams", "clementine", "clove", "cloves",
        "coconut", "coffee", "coleslaw", "cookie", "cookies", "coriander", "corn", "cornbread", "crab", "cracker",
        "crackers", "cranberry", "cranberries", "cream", "cucumber", "cucumbers", "cumin", "curry",
        "dairy", "date", "dates", "dessert", "dice", "dill", "dinner", "dip", "dough", "doughnut", "donut", "dragon fruit",
        "dressing", "drink", "drinks", "duck", "dumpling", "dumplings", "durian", "egg", "eggplant", "eggs", "fennel", "flax", "flour",
        "food", "fruit", "garlic", "ginger", "grape", "grapefruit", "grapes", "gravy", "green bean",
        "green beans", "guava", "ham", "hamburger", "honey", "honeydew", "hot dog", "ice cream",
        "jackfruit", "jam", "jelly", "juice", "kale", "ketchup", "kiwi", "kumquat", "lamb", "langsat", "lasagna", "leek", "leeks",
        "lemon", "lemonade", "lemons", "lemongrass", "lentil", "lentils", "lettuce", "lime", "limes", "lobster", "longan",
        "lunch", "lychee", "macaroni", "mango", "mangoes", "mangosteen", "margarine", "marjoram", "marshmallow", "mayonnaise", "meal",
        "meat", "meatball", "meatballs", "melon", "milk", "mint", "muffin", "muffins", "mushroom",
        "mushrooms", "mustard", "mutton", "noodle", "noodles", "nut", "nutmeg", "nuts", "oatmeal", "oats",
        "oil", "olive", "olives", "omelet", "onion", "onions", "orange", "oranges", "oregano", "oyster",
        "oysters", "pancake", "pancakes", "papaya", "paprika", "parsley", "passion fruit", "pasta", "pastry", "pea", "peach",
        "peaches", "peanut", "peanuts", "pear", "pears", "peas", "pecan", "pecans", "pepper",
        "peppers", "persimmon", "pickle", "pickles", "pie", "pineapple", "pizza", "plum", "plums", "pomegranate", "pomelo", "pork",
        "porridge", "potato", "potatoes", "poultry", "prawn", "prawns", "pretzel", "pretzels",
        "pudding", "pumpkin", "radish", "radishes", "raisin", "raisins", "rambutan", "raspberry", "raspberries",
        "ravioli", "rice", "roast", "roll", "rolls", "rosemary", "saffron", "sage", "salad", "salami", "salmon", "salt",
        "sandwich", "sandwiches", "sauce", "sausage", "sausages", "scallop", "scallops",
        "seafood", "seasoning", "sesame", "shallot", "shallots", "shrimp", "snack", "soda", "soup", "sour cream",
        "soy sauce", "spaghetti", "spice", "spices", "spinach", "squash", "squid", "star anise", "steak",
        "stew", "strawberry", "strawberries", "sugar", "sushi", "syrup", "taco", "tacos",
        "tangerine", "tarragon", "tea", "thyme", "toast", "tofu", "tomato", "tomatoes", "tortilla",
        "trout", "tuna", "turkey", "turmeric", "turnip", "turnips", "vanilla", "veal", "vegetable",
        "vegetables", "vinegar", "waffle", "waffles", "walnut", "walnuts", "wasabi", "water", "watercress", "watermelon",
        "wheat", "wine", "yogurt",
        "beverages", "bin", "busboy", "busser", "candy bar", "cans", "drumsticks", "eggbeater", "figs", "filet", "french fries", "gooseberry", "gourd", "hazelnut", "hostess", "indian jujube", "jujube", "kohlrabi", "lid", "liquor", "lotus rhizome", "manioc", "marang", "menu", "milkshake", "monosodium glutamate", "mulberry", "mussels", "okra", "pastrami", "perilla leaf", "plantain", "plastic wrap", "place setting", "racquetball", "salad bar", "sapodilla", "self-checkout", "shopping cart", "six-packs", "soursop", "spray can", "sugar cane", "sundae", "sweet potato buds", "sweets", "swordfish", "tamarind", "taro", "vendors", "water chestnut", "water mimosa", "whisk", "yam",

        // Animals, Nature & Environment
        "alligator", "animal", "ant", "anteater", "antelope", "ape", "badger", "bat", "bear",
        "beaver", "bee", "beetle", "bird", "bison", "boar", "buffalo", "bull", "butterfly",
        "camel", "cat", "caterpillar", "centipede", "cheetah", "chick", "chicken", "chimpanzee", "cicada", "cobra",
        "cockroach", "cow", "coyote", "crab", "crane", "cricket", "crocodile", "crow", "deer",
        "dinosaur", "dog", "dolphin", "donkey", "dove", "dragonfly", "duck", "eagle", "earthworm", "eel",
        "elephant", "elk", "falcon", "ferret", "finch", "firefly", "fish", "flamingo", "flea",
        "fly", "fox", "frog", "gazelle", "gecko", "giraffe", "goat", "goose", "geese", "gorilla",
        "grasshopper", "gull", "hamster", "hare", "hawk", "hedgehog", "hen", "hippopotamus",
        "hippo", "hornet", "horse", "hound", "hummingbird", "hyena", "iguana", "insect", "jaguar", "jellyfish",
        "kangaroo", "koala", "ladybug", "lamb", "leech", "leopard", "lion", "lizard", "llama", "lobster", "locust",
        "maggot", "mammal", "mantis", "millipede", "mink", "mite", "mole", "monkey", "moose", "mosquito", "moth", "mouse", "mice", "mule",
        "octopus", "ostrich", "otter", "owl", "ox", "oxen", "panda", "panther", "parrot",
        "peacock", "pelican", "penguin", "pig", "pigeon", "platypus", "polar bear", "pony",
        "porcupine", "possum", "python", "quail", "rabbit", "raccoon", "ram", "rat", "raven",
        "reindeer", "reptile", "rhinoceros", "rhino", "rooster", "salamander", "salmon",
        "scorpion", "seagull", "seahorse", "seal", "shark", "sheep", "shrimp", "silkworm", "skunk", "sloth",
        "slug", "snail", "snake", "sparrow", "spider", "squid", "squirrel", "starfish", "stork", "swan",
        "tadpole", "termite", "termites", "tick", "tiger", "toad", "tortoise", "toucan", "trout", "turkey", "turtle", "viper",
        "vulture", "walrus", "wasp", "weasel", "whale", "wolf", "wolves", "wombat", "woodpecker",
        "worm", "yak", "zebra",
        "blackbird", "fawn", "foal", "gibbon", "goldfish", "mammoth", "pangolin", "perch", "piglet", "puma", "stingray",

        // Landscape & Geography
        "air", "bay", "beach", "bush", "canyon", "cave", "cliff", "cloud", "coast", "continent",
        "country", "creek", "desert", "dune", "earth", "field", "fog", "forest", "glacier",
        "grass", "hill", "ice", "island", "jungle", "lake", "land", "meadow", "moon", "mountain",
        "mud", "nature", "ocean", "path", "peak", "pebble", "planet", "plant", "pond", "rain",
        "rainbow", "river", "rock", "sand", "sea", "seashore", "sky", "snow", "soil", "spring",
        "star", "stone", "storm", "stream", "sun", "sunrise", "sunset", "thunder", "thunderstorm", "tide",
        "tree", "valley", "volcano", "waterfall", "wave", "waves", "wind", "wood", "woods",

        // Occupations, People, Family & Society
        "accountant", "actor", "actress", "adult", "architect", "artist", "astronaut", "athlete",
        "attendant", "aunt", "author", "baby", "baker", "banker", "barber", "bartender", "boy", "bride",
        "brother", "builder", "businessman", "businesswoman", "butcher", "captain", "carpenter",
        "carrier", "cashier", "chef", "child", "children", "cleaner", "clerk", "coach", "colleague",
        "conductor", "cook", "cousin", "customer", "dancer", "daughter", "dentist", "detective", "doctor",
        "driver", "electrician", "engineer", "family", "farmer", "father", "fiancé", "fiancée", "firefighter",
        "fireman", "fisherman", "flight attendant", "friend", "gardener", "girl", "grandfather",
        "grandma", "grandmother", "grandpa", "grandparent", "grandson", "granddaughter", "groom", "guest", "guide", "hairdresser", "husband",
        "infant", "inspector", "instructor", "janitor", "journalist", "judge", "lawyer", "librarian",
        "lifeguard", "mail carrier", "mailman", "manager", "man", "men", "mechanic", "miner",
        "model", "mother", "musician", "neighbor", "nephew", "niece", "nurse", "officer",
        "optometrist", "painter", "parent", "parents", "passenger", "patient", "pedestrian",
        "pharmacist", "photographer", "pilot", "plumber", "police officer", "policeman",
        "politician", "postman", "president", "priest", "principal", "professor", "programmer",
        "receptionist", "reporter", "sailor", "salesperson", "scientist", "secretary", "security guard",
        "sibling", "singer", "sister", "soldier", "son", "stepfather", "stepmother", "stepson", "stepdaughter", "student", "surgeon", "tailor", "teacher",
        "technician", "teenager", "therapist", "toddler", "tourist", "trainer", "translator", "traveler", "twin", "twins",
        "uncle", "veterinarian", "vet", "waiter", "waitress", "wife", "witness", "woman",
        "women", "worker", "writer",

        // Sports, Hobbies, Recreation, Arts
        "archery", "badminton", "ball", "balloon", "balloons", "baseball", "basketball", "billiards", "board game", "bowling",
        "boxing", "camping", "canoeing", "chess", "cricket", "cycling", "dancing", "diving",
        "drawing", "exercise", "fishing", "football", "golf", "gym", "gymnastics", "hiking",
        "hockey", "horseback riding", "hunting", "jogging", "judo", "karate", "kayaking",
        "martial arts", "music", "origami", "painting", "photography", "ping pong", "pool",
        "pottery", "reading", "rock climbing", "rollerblading", "rowing", "rugby", "running",
        "sailing", "scuba diving", "sewing", "skateboarding", "skating", "skiing", "snorkeling",
        "snowboarding", "soccer", "surfing", "swimming", "table tennis", "tennis", "track and field",
        "video game", "volleyball", "walking", "water polo", "weightlifting", "wrestling", "yoga",

        // Transportation, Vehicles & City
        "airplane", "airport", "ambulance", "avenue", "bicycle", "bike", "boat", "bridge",
        "building", "bus", "bus stop", "car", "crosswalk", "ferry", "fire engine", "gas station",
        "helicopter", "highway", "intersection", "lane", "lorry", "metro", "motorcycle",
        "parking lot", "pavement", "railroad", "railway", "road", "roundabout", "scooter",
        "ship", "sidewalk", "sign", "station", "stoplight", "street", "subway", "taxi",
        "traffic", "traffic light", "train", "tram", "truck", "tunnel", "van", "vehicle", "yacht",

        // Communication, Office, School, Technology & Tools
        "address", "algebra", "algorithm", "alphabet", "antenna", "app", "application", "archive", "area code", "arrow", "attachment",
        "axe", "backpack", "badge", "bagger", "ballpoint", "battery", "beaker", "belt", "belts", "binder clip", "blackboard", "bleachers", "bolt", "book", "bookmark", "bradawl", "briefcase", "browser", "burner", "button", "buttons", "calculator",
        "calendar", "camera", "candlestick", "canister", "card", "cell phone", "certificate", "chalk", "charger", "chisel", "clamp", "clip", "clips",
        "compass", "computer", "cord", "cords", "cpu", "credit", "credit card", "cursor", "cutter", "cutters", "cylinder", "database", "deodorant", "diaper", "diapers", "dictionary", "display", "document", "drill", "drive", "drives", "dvd",
        "email", "envelope", "eraser", "eyeball", "file", "flute", "folder", "fountain", "gauze", "gears", "globe", "glue", "goggles", "guitar", "gutters", "hammer", "handrail", "hardware", "headphones", "headlights", "highlighter",
        "high school", "hubcap", "jigsaw", "kazoo", "keyboard", "keychain", "keyhole", "keyring", "kid", "kids", "kiosk", "laptop", "laser", "letter", "locker", "lockers", "magazine", "mail", "mailbox", "manual", "map",
        "marker", "megaphone", "memo", "microchip", "microphone", "microprocessor", "microscope", "monitor", "mousepad", "muffler", "nail", "net",
        "network", "notebook", "notepad", "nut", "outlet", "pacifier", "packet", "pad", "pads", "page", "paint", "paintbrush",
        "paper", "paperclip", "parachute", "passport", "password", "pen", "pencil", "pencil sharpener", "pendant", "pendulum", "percolator", "perfume", "phonics",
        "piano", "picture", "pier", "plaque", "plaster", "pliers", "plug", "pocketbook", "podium", "popcorn", "port", "ports", "postage", "postcard", "poster", "potholder", "powder", "press", "printer", "projector", "propane", "pulley", "pump", "punch", "puppet", "pushpin", "puzzle", "pyramid", "radio", "raft", "rail", "rails", "receiver", "reflector", "register", "remote", "report", "ribbon", "rifle", "rivet", "root", "rotor", "rowboat", "rubies", "ruler", "saddle", "sailboat", "saw", "saxophone", "scanner",
        "screen", "screw", "screwdriver", "server", "shampoo", "shawl", "sickle", "silo", "skateboard", "skyscraper", "sling", "smock", "snowmobile", "software", "socket", "speedometer", "spray", "spurs", "stapler", "staples",
        "stationery", "straw", "suit", "suitcase", "sunroof", "sunscreen", "surfboard", "surgery", "swimming pool", "switch", "switchblade", "switches", "tanker", "tape", "tape measure", "taxicab", "telephone", "telescope", "tent", "textbook", "thermometer", "thumbtack", "tile", "tire", "phone", "phones", "toolbox", "toothbrush", "toothpaste",
        "tools", "touchscreen", "tractor", "trash", "tricycle", "triplets", "trombone", "trumpet", "tuba", "tugboat", "turntable", "typewriter", "unicycle", "usb", "vial", "viola", "violin", "visor", "walkway", "watchband", "website", "weed", "whiteboard", "windmill", "windshield", "wiper", "wire", "wrench", "wristwatch", "x-ray", "xylophone",
        "bangs", "bib", "bulletin board", "cable", "cafeteria", "calculus", "cologne", "corn rows", "correction fluid", "denominator", "discipline", "dormitory", "dust", "endpoint", "evening", "fahrenheit", "firstname", "flame", "geometry", "graph", "headset", "hailstorm", "inch", "keypad", "landlord", "lastname", "lightning", "menu bar", "mess", "middle initial", "midnight", "motherboard", "negative integers", "noon", "numerator", "operator", "parallelogram", "peephole", "peninsula", "perimeter", "plains", "playground", "pointer", "popup ad", "positive integers", "price tag", "printout", "quad", "quotient", "radius", "rectangle", "restrooms", "rolodex", "roommates", "scroll bar", "sex", "smartphone", "stepbrother", "stepsister", "sum", "tab", "tattoo", "teacher's aide", "aide", "call", "couple", "blizzard", "snowstorm", "handset", "intercom", "quilt", "comforter", "potty", "cockroaches", "roaches", "lead", "cord", "prostate", "uterus", "womb", "collarbone", "clavicle", "saliva", "perspiration", "mandarin", "litchi", "acerola", "beetroot", "scallions", "spud", "daikon", "fish mint", "pennywort", "centella", "aftershave", "slacks", "zip", "pay phone", "payphone", "tennis racket", "test booklet", "typist", "utilities", "water polo", "waterpolo", "weekdays", "wireless headset", "workbook",

        // Miscellaneous abstract / general nouns
        "ability", "accent", "accident", "action", "activity", "advantage", "advice", "afternoon", "age",
        "agreement", "amount", "angle", "angles", "anniversary", "answer", "appointment", "area", "argument", "arrangement",
        "art", "atmosphere", "attention", "attitude", "audience", "authority", "award",
        "behavior", "benefit", "bill", "birth", "birthday", "block", "body", "border",
        "bottom", "box", "brand", "bubble", "business", "care", "career", "case", "cause", "center",
        "century", "ceremony", "chance", "change", "character", "charge", "choice", "circle",
        "citizen", "city", "claim", "class", "climate", "club", "code", "collection", "color",
        "comfort", "command", "comment", "company", "competition", "complaint", "condition",
        "connection", "consequence", "control", "conversation", "corner", "cost", "counsel",
        "countryside", "courage", "course", "court", "crime", "crowd", "culture", "danger",
        "data", "date", "day", "deal", "death", "decision", "defense", "degree", "delay",
        "delivery", "demand", "department", "depth", "description", "design", "detail",
        "device", "difference", "difficulty", "direction", "discount", "discovery", "discussion",
        "disease", "distance", "district", "division", "doubt", "draft", "drama", "dream",
        "duty", "economy", "edge", "education", "effect", "effort", "election", "electricity",
        "emergency", "emotion", "emphasis", "employment", "energy", "engine", "enjoyment",
        "entertainment", "entry", "environment", "episode", "equipment", "error", "escape",
        "essay", "estate", "event", "evidence", "example", "exchange", "excitement", "excuse",
        "exhibition", "expense", "experience", "experiment", "expert", "explanation", "facility",
        "fact", "factor", "factory", "failure", "fair", "faith", "fame", "fashion", "fault",
        "favor", "fear", "feature", "fee", "feeling", "festival", "fiction", "figure", "film",
        "finance", "flight", "flow", "focus", "force", "form", "fortune", "foundation", "freedom",
        "front", "fuel", "fun", "function", "fund", "future", "game", "gap", "garbage",
        "generation", "gift", "goal", "government", "grade", "graduate", "grant", "grocery",
        "ground", "group", "growth", "guarantee", "guard", "guidebook", "habit", "half",
        "handicap", "happiness", "harm", "health", "hearing", "heart", "heat", "height",
        "heritage", "hero", "history", "hobby", "hold", "hole", "holiday", "home", "homework",
        "honesty", "honor", "hook", "hope", "horizon", "horror", "hospital", "host", "hotel",
        "hour", "house", "household", "housing", "humor", "hunger", "hygiene", "idea",
        "identity", "illness", "image", "imagination", "impact", "importance", "impression",
        "improvement", "incident", "income", "increase", "independence", "index", "industry",
        "infection", "inflation", "influence", "information", "injury", "inquiry", "inspection",
        "instance", "institution", "insurance", "intention", "interest", "interview",
        "introduction", "invention", "investment", "invitation", "irony", "island", "issue",
        "item", "jealousy", "jewel", "jewelry", "job", "joke", "journal", "journey", "joy",
        "judgment", "justice", "key", "kind", "kindness", "king", "kingdom", "kiss", "knowledge",
        "label", "labor", "laboratory", "lack", "lake", "language", "laughter", "law",
        "leader", "leadership", "league", "leak", "learning", "leather", "lecture", "legend",
        "length", "lesson", "level", "liberty", "library", "license", "life", "lifestyle",
        "limit", "line", "link", "liquid", "list", "literature", "location", "logic", "loss",
        "love", "luck", "luggage", "luxury", "machine", "magic", "mail", "majority", "make",
        "management", "manner", "manual", "manufacturer", "mark", "market", "marriage",
        "mask", "mass", "master", "match", "material", "math", "mathematics", "matter",
        "maximum", "meaning", "means", "measure", "measurement", "mechanism", "media",
        "medicine", "membership", "memory", "merit", "message", "metal", "method", "middle",
        "mind", "minimum", "minister", "ministry", "minute", "miracle", "mission", "mistake",
        "mixture", "mode", "moment", "money", "month", "monument", "mood", "moral", "morning",
        "motion", "motivation", "motor", "movement", "movie", "museum", "mystery", "name",
        "nation", "nationality", "native", "necessity", "need", "neighborhood", "nerve",
        "nest", "news", "newspaper", "night", "noise", "nonsense", "norm", "north", "note",
        "notice", "novel", "number", "object", "objective", "obligation", "observation",
        "occasion", "occupation", "ocean", "offense", "offer", "office", "official", "operation",
        "opinion", "opportunity", "opposite", "option", "orange", "order", "ordinary",
        "organization", "origin", "outcome", "outlet", "output", "oven", "owner", "ownership",
        "oxygen", "pace", "package", "page", "pain", "paint", "pair", "palace", "panel",
        "paperwork", "parade", "paragraph", "parallel", "pardon", "part", "partner", "partnership",
        "party", "passage", "passion", "path", "patience", "pattern", "pause", "payment",
        "peace", "peer", "penalty", "pencil", "pension", "people", "percentage", "perception",
        "performance", "period", "permission", "permit", "personality", "perspective", "pet",
        "phase", "phenomenon", "philosophy", "photo", "phrase", "physics", "picture", "piece",
        "pilot", "pin", "place", "plan", "plane", "planning", "plastic", "platform", "player",
        "pleasure", "plot", "poem", "poet", "poetry", "point", "pole", "police", "policy",
        "pollution", "pool", "population", "portion", "portrait", "position", "possession",
        "possibility", "post", "postage", "poster", "potential", "power", "practice", "praise",
        "prayer", "preference", "preparation", "presence", "presentation", "presidency",
        "pressure", "price", "pride", "priest", "principle", "priority", "prison", "prisoner",
        "privacy", "privilege", "prize", "problem", "procedure", "process", "produce", "producer",
        "product", "production", "profession", "professor", "profit", "program", "programme",
        "progress", "project", "promise", "promotion", "proof", "property", "proportion",
        "proposal", "prospect", "protection", "protest", "protocol", "provision", "psychology",
        "pub", "public", "publication", "publicity", "publisher", "purchase", "purpose",
        "qualification", "quality", "quantity", "quarter", "queen", "question", "queue",
        "quotation", "quote", "race", "rage", "range", "rank", "rate", "rating", "ratio",
        "reaction", "reality", "reason", "receipt", "receiver", "reception", "recipe",
        "recognition", "recommendation", "record", "recovery", "reduction", "reference",
        "reflection", "reform", "refugee", "region", "register", "registration", "regret",
        "regular", "regulation", "reinforcement", "relationship", "relative", "release",
        "relief", "religion", "remedy", "reminder", "remote", "rent", "repair", "repeat",
        "replacement", "reply", "report", "reputation", "request", "requirement", "rescue",
        "research", "reserve", "resident", "resistance", "resolution", "resort", "resource",
        "respect", "response", "responsibility", "rest", "restaurant", "result", "retail",
        "retirement", "return", "revenue", "review", "revolution", "reward", "rhythm",
        "rider", "ridge", "rifle", "right", "risk", "ritual", "rival", "river", "role",
        "romance", "route", "routine", "rule", "ruler", "rumor", "rush", "sacrifice",
        "safety", "salary", "sale", "sample", "sanction", "satisfaction", "saving", "savings",
        "scale", "scenario", "scene", "schedule", "scheme", "scholar", "scholarship", "school",
        "science", "scientist", "scope", "score", "screen", "search", "season", "seat",
        "second", "secret", "secretary", "section", "sector", "security", "seed", "segment",
        "selection", "self", "semester", "seminar", "senior", "sensation", "sense", "sentence",
        "sequence", "series", "servant", "service", "session", "settlement", "shame", "shape",
        "share", "shelter", "shift", "shine", "shock", "shop", "shopping", "shore", "shortage",
        "shot", "show", "shower", "side", "sigh", "sight", "signal", "signature", "significance",
        "silence", "silk", "similarity", "simplicity", "sin", "sir", "sister", "site",
        "situation", "size", "skill", "slave", "slavery", "sleep", "slice", "slide", "slope",
        "slot", "smell", "smile", "smoke", "snow", "society", "software", "soil", "soldier",
        "solution", "song", "sorrow", "sort", "sound", "source", "space", "speaker", "specialist",
        "species", "speech", "speed", "spell", "spending", "sphere", "spirit", "spite", "split",
        "sport", "spot", "spread", "spring", "square", "stadium", "staff", "stage", "standard",
        "star", "state", "statement", "station", "statistics", "status", "statue", "stay",
        "steel", "stem", "step", "stick", "stimulus", "stock", "stomach", "storage", "store",
        "storm", "story", "strategy", "strength", "stress", "strike", "string", "stroke",
        "structure", "struggle", "student", "studio", "study", "stuff", "style", "subject",
        "substance", "substitute", "success", "suggestion", "suit", "summary", "summer",
        "summit", "sun", "sunlight", "supermarket", "supply", "support", "supporter", "surface",
        "surprise", "survey", "survival", "survivor", "suspect", "swimming", "switch", "symbol",
        "sympathy", "symptom", "system", "table", "tactic", "tail", "talent", "talk", "tank",
        "target", "task", "taste", "tax", "taxi", "taxpayer", "tea", "teaching", "team",
        "tear", "technology", "temperature", "temple", "tendency", "tennis", "tension", "term",
        "terms", "territory", "terror", "terrorism", "terrorist", "test", "testimony", "text",
        "texture", "thanks", "theater", "theme", "theory", "therapy", "thing", "things",
        "thinking", "threat", "threshold", "throat", "thumb", "ticket", "time", "timing",
        "tip", "tissue", "title", "today", "tomorrow", "tone", "tongue", "tool", "tooth",
        "top", "topic", "total", "touch", "tour", "tourism", "tourist", "tournament", "towel",
        "tower", "town", "toy", "trace", "track", "trade", "tradition", "traffic", "tragedy",
        "trail", "train", "trainer", "training", "trait", "transaction", "transfer", "transition",
        "translation", "transport", "transportation", "trap", "trash", "travel", "treat",
        "treatment", "treaty", "tree", "trend", "trial", "triangle", "tribe", "trick", "trip",
        "triumph", "troop", "trouble", "truck", "trust", "truth", "tube", "tuition", "tumor",
        "tune", "tunnel", "turn", "twist", "type", "umbrella", "uncertainty", "uncle",
        "understanding", "union", "unit", "universe", "university", "upgrade", "usage", "user",
        "vacation", "value", "values", "van", "variety", "vegetable", "vehicle", "venture",
        "venue", "verdict", "version", "vessel", "victim", "victory", "video", "view", "viewer",
        "village", "violence", "virtue", "virus", "visa", "vision", "visit", "visitor",
        "visual", "voice", "volume", "volunteer", "vote", "voter", "voting", "voyage",
        "wage", "wages", "wait", "waiter", "walk", "wall", "wallet", "war", "ward", "warmth",
        "warning", "waste", "watch", "water", "wave", "way", "weakness", "wealth", "weapon",
        "weather", "wedding", "week", "weekend", "weight", "welcome", "welfare", "west",
        "wheat", "wheel", "whisper", "width", "wife", "wildlife", "will", "willingness",
        "win", "wind", "window", "wine", "wing", "winner", "winter", "wire", "wisdom",
        "wish", "witness", "woman", "wonder", "wood", "wooden", "word", "work", "worker",
        "working", "workout", "workplace", "world", "worry", "worth", "wound", "writer",
        "writing", "yard", "year", "yesterday", "yield", "youth", "zone"
    )

    // Common Verbs
    private val commonVerbs = setOf(
        "accept", "achieve", "act", "add", "admit", "affect", "afford", "agree", "aim", "allow",
        "announce", "answer", "apologize", "appear", "apply", "appoint", "appreciate", "approach", "approve",
        "argue", "arm", "arrange", "arrive", "ask", "assist", "assume", "attach", "attack", "attempt",
        "attend", "attract", "avoid", "bake", "base", "bear", "beat", "become", "begin",
        "behave", "believe", "belong", "bend", "bet", "bid", "bind", "bite", "bleed", "blend",
        "block", "blow", "boil", "borrow", "bother", "bounce", "bow", "brake", "break", "breathe", "breed", "bring", "brush", "bubble", "build", "burn", "burst", "bury", "buy",
        "calculate", "call", "calm", "care", "carry", "catch", "cause", "cease", "celebrate",
        "challenge", "change", "charge", "chase", "chat", "check", "cheer", "choose", "chop", "chuckle", "circle",
        "cite", "claim", "clap", "clean", "clear", "climb", "cling", "close", "coach",
        "collapse", "collect", "combine", "come", "command", "commit", "communicate", "compare",
        "compete", "complain", "complete", "compose", "concentrate", "conclude", "conduct",
        "confirm", "connect", "consider", "consist", "construct", "consult", "consume", "contact",
        "contain", "continue", "contribute", "control", "convert", "convince", "cook", "cool",
        "cope", "copy", "correct", "cost", "cough", "count", "cover", "crack", "crash",
        "crawl", "create", "creep", "cross", "cry", "cure", "curl", "cut", "damage", "dance",
        "dare", "deal", "debate", "decide", "declare", "decline", "decorate", "decrease",
        "defend", "define", "delay", "deliver", "demand", "demonstrate", "deny", "depend",
        "deposit", "describe", "deserve", "design", "desire", "destroy", "detect", "determine",
        "develop", "devote", "dial", "dice", "dictate", "die", "differ", "dig", "direct", "disagree", "disappear",
        "disappoint", "discover", "discuss", "dislike", "display", "dissolve", "distinguish",
        "distribute", "dive", "divide", "divorce", "do", "donate", "doubt", "drag", "draw",
        "dream", "dress", "drift", "drink", "drive", "drop", "drown", "dry", "dust", "earn",
        "eat", "educate", "elect", "eliminate", "embarrass", "emerge", "emphasize", "employ",
        "empty", "enable", "encourage", "end", "endure", "engage", "enhance", "enjoy", "ensure",
        "enter", "entertain", "equip", "escape", "establish", "estimate", "evaluate", "examine",
        "exceed", "exchange", "excite", "exclude", "excuse", "execute", "exercise", "exhibit",
        "exist", "expand", "expect", "experience", "explain", "explode", "explore", "export",
        "expose", "express", "extend", "face", "fail", "faint", "fall", "fancy", "fasten",
        "favour", "fear", "feed", "feel", "fetch", "fight", "figure", "fill", "film", "filter",
        "find", "fine", "finger", "finish", "fire", "fit", "fix", "flash", "flee", "float",
        "flood", "flow", "fly", "focus", "fold", "follow", "forbid", "force", "forecast",
        "forget", "forgive", "form", "found", "frame", "freeze", "frighten", "fry", "fulfill",
        "gain", "gather", "gaze", "generate", "get", "give", "glance", "glow", "glue", "go",
        "govern", "grab", "graduate", "grant", "grasp", "greet", "grin", "grip", "grow",
        "guarantee", "guard", "guess", "guide", "hammer", "hand", "handle", "hang", "happen",
        "harm", "harvest", "hate", "haunt", "have", "head", "heal", "hear", "heat", "help",
        "hesitate", "hide", "highlight", "hike", "hire", "hit", "hold", "hook", "hope",
        "host", "house", "hug", "hunt", "hurry", "hurt", "identify", "ignore", "illustrate",
        "imagine", "imitate", "implement", "imply", "import", "impose", "impress", "improve",
        "include", "incorporate", "increase", "indicate", "influence", "inform", "inherit",
        "injure", "insist", "inspect", "inspire", "install", "instruct", "insure", "intend",
        "interact", "interest", "interfere", "interpret", "interrupt", "introduce", "invent",
        "invest", "investigate", "invite", "involve", "iron", "irritate", "isolate", "issue",
        "join", "joke", "judge", "jump", "justify", "keep", "kick", "kill", "kiss", "kneel",
        "knit", "knock", "know", "label", "lack", "land", "last", "laugh", "launch", "lay",
        "lead", "leak", "lean", "leap", "learn", "leave", "lecture", "lend", "let", "level",
        "license", "lick", "lie", "lift", "light", "like", "limit", "link", "listen", "live",
        "load", "loan", "locate", "lock", "lodge", "log", "look", "lose", "love", "lower",
        "maintain", "make", "manage", "manufacture", "march", "mark", "market", "marry",
        "mask", "match", "matter", "mature", "mean", "measure", "meet", "melt", "mention",
        "merge", "mind", "miss", "mistake", "mix", "modify", "monitor", "mop", "mount",
        "move", "multiply", "murder", "name", "navigate", "need", "neglect", "negotiate",
        "nod", "nominate", "note", "notice", "notify", "obey", "object", "oblige", "observe",
        "obtain", "occupy", "occur", "offend", "offer", "open", "operate", "oppose", "order",
        "organize", "originate", "outline", "overcome", "overlook", "owe", "own", "pack",
        "paddle", "paint", "park", "participate", "pass", "pat", "pause", "pay", "peel",
        "perform", "permit", "persuade", "photograph", "pick", "pile", "pilot", "pin",
        "pinch", "place", "plan", "plant", "play", "plead", "please", "pledge", "plug",
        "plunge", "point", "polish", "poll", "pop", "pose", "position", "possess", "post",
        "pour", "practice", "praise", "pray", "preach", "precede", "predict", "prefer",
        "prepare", "prescribe", "present", "preserve", "press", "pretend", "prevent", "price",
        "print", "proceed", "process", "produce", "program", "progress", "prohibit", "project",
        "promise", "promote", "prompt", "pronounce", "propose", "protect", "protest", "prove",
        "provide", "provoke", "publish", "pull", "pump", "punch", "punish", "purchase",
        "pursue", "push", "put", "qualify", "question", "queue", "quit", "quote", "race",
        "rain", "raise", "range", "rank", "rate", "reach", "react", "read", "realize",
        "reassure", "rebel", "recall", "receive", "reckon", "recognize", "recommend", "reconcile",
        "record", "recover", "recruit", "reduce", "refer", "reflect", "refuse", "regard",
        "regret", "regulate", "reinforce", "reject", "relate", "relax", "release", "relieve",
        "rely", "remain", "remark", "remember", "remind", "remove", "render", "renew",
        "rent", "repair", "repeat", "replace", "reply", "report", "represent", "reproduce",
        "request", "require", "rescue", "research", "resemble", "reserve", "reside", "resist",
        "resolve", "resort", "respect", "respond", "rest", "restore", "restrict", "result",
        "resume", "retain", "retire", "retreat", "return", "reveal", "reverse", "review",
        "revise", "revive", "reward", "rid", "ride", "ring", "rinse", "rip", "rise", "risk",
        "rob", "rock", "roll", "rot", "row", "rub", "ruin", "rule", "run", "rush", "sack",
        "sail", "satisfy", "save", "say", "scan", "scare", "scatter", "schedule", "score",
        "scrape", "scratch", "scream", "screen", "screw", "scrub", "seal", "search", "seat",
        "secure", "see", "seek", "seem", "seize", "select", "sell", "send", "sense", "sentence",
        "separate", "serve", "service", "set", "settle", "sew", "shake", "shape", "share",
        "shave", "shear", "shed", "shift", "shine", "ship", "shock", "shoot", "shop", "shout",
        "show", "shrink", "shut", "sigh", "sign", "signal", "silence", "simplify", "sin",
        "sing", "sink", "sit", "skate", "ski", "skip", "slap", "slash", "slaughter", "sleep",
        "slice", "slide", "slip", "slow", "smash", "smell", "smile", "smoke", "snap", "snatch",
        "sneeze", "sniff", "snore", "snow", "soak", "solve", "soothe", "sort", "sound",
        "sow", "span", "spare", "spark", "sparkle", "speak", "specify", "speed", "spell",
        "spend", "spill", "spin", "spit", "splash", "split", "spoil", "spot", "spray", "spread",
        "spring", "sprinkle", "squeeze", "stab", "stain", "stamp", "stand", "stare", "start",
        "starve", "state", "stay", "steal", "steam", "steer", "step", "stick", "stimulate",
        "stir", "stitch", "stop", "store", "strain", "stray", "strengthen", "stress", "stretch",
        "strike", "string", "strip", "strive", "stroke", "structure", "struggle", "study",
        "stuff", "stumble", "stun", "style", "submit", "subscribe", "substitute", "succeed",
        "suck", "sue", "suffer", "suggest", "suit", "summarize", "supervise", "supplement",
        "supply", "support", "suppose", "suppress", "surf", "surrender", "surround", "survey",
        "survive", "suspect", "suspend", "sustain", "swallow", "swear", "sweat", "sweep",
        "swell", "swim", "swing", "switch", "tackle", "take", "talk", "tame", "tap", "target",
        "taste", "tax", "teach", "tear", "tease", "tell", "tempt", "tend", "terminate",
        "test", "testify", "thank", "thaw", "think", "thrive", "throw", "thrust", "tick",
        "tickle", "tidy", "tie", "tighten", "tilt", "time", "tip", "tire", "toast", "tolerate",
        "toss", "touch", "tour", "tow", "trace", "track", "trade", "train", "transfer",
        "transform", "translate", "transmit", "transport", "trap", "travel", "tread", "treat",
        "tremble", "trick", "trigger", "trim", "trip", "trot", "trouble", "trust", "try",
        "tuck", "tug", "tumble", "tune", "turn", "twist", "type", "undergo", "understand",
        "undertake", "undo", "unfold", "unite", "unlock", "unpack", "untie", "unveil",
        "update", "upgrade", "uphold", "upset", "urge", "use", "utilize", "utter", "vacuum", "value",
        "vanish", "vary", "venture", "view", "violate", "visit", "voice", "volunteer", "vote",
        "vow", "wade", "wait", "wake", "walk", "wander", "want", "warm", "warn", "wash",
        "waste", "watch", "water", "wave", "wear", "weave", "wed", "weep", "weigh", "welcome",
        "whip", "whisper", "whistle", "widen", "win", "wind", "wink", "wipe", "wire", "wish",
        "withdraw", "withhold", "withstand", "witness", "wonder", "work", "worry", "worship",
        "wound", "wrap", "wreck", "wrestle", "wriggle", "wring", "write", "yawn", "yell",
        "yield", "zip", "zoom",
        "bathe", "broil", "dribble", "erase", "gargle", "grate", "immigrate", "misbehave", "mow", "subtract", "underline", "undress", "unravel", "unscramble", "remarry", "simmer"
    )

    // Common Adjectives
    private val commonAdjectives = setOf(
        "able", "absent", "absolute", "accurate", "active", "actual", "acute", "adaptable",
        "adequate", "afraid", "aggressive", "alive", "alone", "alternative", "amazing",
        "ambitious", "ancient", "angry", "annual", "anxious", "apparent", "appropriate",
        "artificial", "ashamed", "asleep", "attractive", "automatic", "available", "average",
        "awake", "aware", "awful", "awkward", "bad", "bare", "basic", "beautiful", "big",
        "bitter", "bizarre", "black", "bland", "blank", "blind", "blonde", "blue", "blunt",
        "bold", "bored", "boring", "brave", "brief", "bright", "brilliant", "broad", "broken",
        "brown", "busy", "calm", "capable", "careful", "careless", "casual", "cautious",
        "central", "certain", "cheap", "cheerful", "chemical", "chief", "chilly", "classic",
        "clean", "clear", "clever", "close", "cloudy", "clumsy", "cold", "colorful", "comfortable",
        "common", "compact", "competent", "complete", "complex", "complicated", "confident",
        "confused", "conscious", "considerable", "constant", "contemporary", "content", "convenient",
        "cool", "cordless", "correct", "corrupt", "cozy", "crazy", "creative", "creepy", "critical",
        "crucial", "crude", "cruel", "curious", "curly", "current", "cute", "daily", "damp",
        "dangerous", "dark", "dead", "deaf", "dear", "decent", "decisive", "deep", "definite",
        "delicate", "delicious", "delighted", "dense", "dependent", "depressed", "desirable",
        "desperate", "detailed", "determined", "different", "difficult", "digital", "dim",
        "diplomatic", "direct", "dirty", "disabled", "disappointed", "disastrous", "disposable", "distant",
        "distinct", "diverse", "divine", "dizzy", "domestic", "dominant", "double", "doubtful",
        "dramatic", "dreary", "drunk", "dry", "dual", "due", "dull", "dumb", "dynamic",
        "eager", "early", "earnest", "easy", "economic", "efficient", "elaborate", "elastic",
        "elderly", "electric", "electrical", "electronic", "elegant", "elementary", "eligible",
        "embarrassed", "emergency", "eminent", "emotional", "empty", "energetic", "enormous",
        "enthusiastic", "entire", "equal", "equivalent", "essential", "eternal", "ethical",
        "ethnic", "eventual", "evident", "evil", "exact", "excellent", "exceptional", "excessive",
        "excited", "exciting", "exclusive", "exotic", "expensive", "experienced", "expert",
        "explicit", "extra", "extraordinary", "extreme", "fabulous", "faint", "fair", "faithful",
        "fake", "false", "familiar", "famous", "fancy", "fantastic", "far", "fascinating",
        "fashionable", "fast", "fat", "fatal", "favorable", "favorite", "feasible", "feeble",
        "female", "ferocious", "fertile", "festive", "few", "fierce", "final", "financial",
        "fine", "firm", "first", "fit", "fixed", "flat", "flexible", "flimsy", "fluent",
        "fluid", "flying", "fond", "foolish", "foreign", "formal", "former", "fortunate",
        "forward", "fragile", "frank", "free", "frequent", "fresh", "friendly", "frightened",
        "frightening", "front", "frozen", "fruitful", "frustrated", "full", "fun", "fundamental",
        "funny", "furious", "future", "general", "generous", "gentle", "genuine", "giant",
        "gifted", "gigantic", "glad", "glamorous", "global", "gloomy", "glorious", "gold",
        "golden", "good", "gorgeous", "graceful", "gradual", "grand", "grateful", "grave",
        "gray", "great", "greedy", "green", "grey", "grief", "grim", "gross", "guilty",
        "handy", "handsome", "happy", "hard", "harmful", "harmless", "harsh", "hasty",
        "haughty", "healthy", "heavy", "helpful", "helpless", "hidden", "hideous", "high",
        "hilarious", "historic", "historical", "hollow", "holy", "honest", "hopeful", "hopeless",
        "horrible", "hostile", "hot", "huge", "human", "humble", "humid", "hungry", "hurt",
        "ideal", "identical", "idle", "ignorant", "ill", "illegal", "illiterate", "illogical",
        "immense", "imminent", "immune", "impatient", "imperfect", "implicit", "important",
        "impossible", "impressive", "improper", "impulsive", "inadequate", "inappropriate",
        "incapable", "incident", "inclined", "inclusive", "incompetent", "incomplete", "incredible",
        "independent", "indifferent", "indirect", "individual", "indoor", "industrial",
        "inevitable", "infamous", "inferior", "infinite", "informal", "inherent", "initial",
        "injured", "innocent", "innovative", "inside", "insightful", "insignificant", "instant",
        "instinctive", "insufficient", "integral", "intellectual", "intelligent", "intense",
        "intent", "interactive", "interested", "interesting", "interior", "intermediate",
        "internal", "international", "intimate", "intricate", "intriguing", "intrinsic",
        "invalid", "invaluable", "invisible", "involved", "ironic", "irrational", "irregular",
        "irrelevant", "irresponsible", "isolated", "jealous", "joint", "jolly", "joyful",
        "jubilant", "judgmental", "judicial", "juicy", "junior", "just", "keen", "key",
        "kind", "knowing", "knowledgeable", "known", "laborious", "lack", "lame", "large",
        "last", "late", "latent", "lateral", "latter", "lavish", "lawful", "lazy", "leading",
        "lean", "learned", "least", "legal", "legendary", "legitimate", "leisurely", "lengthy",
        "lenient", "less", "lesser", "lethal", "level", "liable", "liberal", "liberated",
        "light", "likely", "limited", "linear", "linguistic", "liquid", "literal", "literary",
        "literate", "little", "live", "lively", "living", "local", "logical", "lonely",
        "long", "loose", "lost", "loud", "lovely", "low", "loyal", "lucid", "lucky", "luminous",
        "mad", "magic", "magical", "magnificent", "main", "major", "male", "malicious",
        "mandatory", "marginal", "marine", "marked", "married", "marvelous", "masculine",
        "massive", "master", "matching", "material", "maternal", "mathematical", "mature",
        "maximum", "meager", "mean", "meaningful", "meaningless", "measurable", "mechanical",
        "medical", "medieval", "mediocre", "medium", "melancholy", "mellow", "melodic",
        "memorable", "mental", "merciful", "mere", "merry", "messy", "metallic", "methodical",
        "meticulous", "microscopic", "middle", "mighty", "mild", "military", "minimal",
        "minimum", "minor", "minute", "miserable", "misleading", "missing", "mixed", "mobile",
        "mock", "moderate", "modern", "modest", "moist", "momentary", "monotonous", "monstrous",
        "monthly", "monumental", "moral", "morbid", "mortal", "motherly", "motionless",
        "motivating", "movable", "moving", "much", "muddy", "multiple", "municipal", "musical",
        "mute", "mutual", "mysterious", "mythical", "naive", "naked", "narrow", "nasty",
        "national", "native", "natural", "naughty", "naval", "near", "neat", "necessary",
        "negative", "negligent", "nervous", "neutral", "new", "next", "nice", "nimble",
        "noble", "noisy", "nominal", "nonchalant", "normal", "northern", "notable", "noted",
        "noticeable", "notorious", "novel", "novice", "nuclear", "null", "numb", "numerical",
        "numerous", "obedient", "objective", "obligatory", "oblivious", "obnoxious", "obscure",
        "observant", "obsolete", "obstinate", "obvious", "occasional", "occupied", "odd",
        "offensive", "official", "old", "ominous", "ongoing", "online", "open", "operational",
        "opposing", "opposite", "optimal", "optimistic", "optional", "oral", "orange",
        "ordinary", "organic", "organizational", "original", "ornate", "orthodox", "outdoor",
        "outer", "outrageous", "outspoken", "outstanding", "outward", "overall", "overdue",
        "overnight", "overseas", "overwhelming", "painful", "painless", "pale", "parallel",
        "paramount", "paranoid", "partial", "particular", "passionate", "passive", "past",
        "patient", "patriotic", "peaceful", "peculiar", "pedestrian", "peerless", "penal",
        "pending", "penitent", "pensive", "perceptive", "perfect", "perilous", "periodic",
        "permanent", "permissible", "perpetual", "perplexed", "persistent", "personal",
        "persuasive", "pessimistic", "petty", "petrified", "petulant", "philosophical",
        "physical", "picturesque", "pink", "pious", "pitiful", "pivotal", "plain", "plastic",
        "plausible", "playful", "pleasant", "pleased", "pleasing", "plentiful", "pliant",
        "plucky", "plummeting", "poetic", "poignant", "pointless", "poisonous", "polite",
        "political", "poor", "popular", "portable", "positive", "possible", "potential",
        "powerful", "practical", "precious", "precise", "predictable", "predominant", "pregnant",
        "prehistoric", "preliminary", "premature", "premier", "premium", "preoccupied", "prepared",
        "prerequisite", "present", "presidential", "pressing", "prestigious", "pretty", "previous",
        "priceless", "prickly", "primary", "prime", "primitive", "principal", "prior",
        "pristine", "private", "probable", "problematic", "procedural", "productive", "professional",
        "proficient", "profound", "progressive", "prolific", "prominent", "promising", "prompt",
        "prone", "proper", "prophetic", "proportional", "prospective", "prosperous", "protective",
        "proud", "provocative", "prudent", "psychological", "public", "punctual", "pure",
        "purple", "purposeful", "puzzled", "quaint", "qualified", "qualitative", "quantitative",
        "quarrelsome", "quick", "quiet", "quizzical", "radical", "radiant", "radioactive",
        "ragged", "random", "rapid", "rare", "rash", "rational", "raw", "ready", "real",
        "realistic", "reasonable", "reassuring", "recent", "receptive", "reckless", "recognizable",
        "recurrent", "red", "redundant", "refined", "reflective", "reflexive", "reformist",
        "refreshing", "regardless", "regional", "regretful", "regular", "regulatory", "relatable",
        "relative", "relaxed", "relentless", "relevant", "reliable", "reliant", "relieved",
        "reluctant", "remarkable", "remedial", "remote", "removable", "renowned", "repetitive",
        "representative", "repressed", "repulsive", "reputable", "required", "requisite",
        "reserved", "residential", "resilient", "resolute", "resourceful", "respectable",
        "respectful", "respective", "responsible", "responsive", "restful", "restless",
        "restrictive", "retail", "retained", "retiring", "retro", "retrospective", "revealing",
        "reverent", "revolutionary", "rewarding", "rhetorical", "rich", "ridiculous", "right",
        "righteous", "rigid", "rigorous", "ripe", "risky", "ritual", "rival", "robust",
        "romantic", "rotten", "rough", "round", "routine", "royal", "rude", "rugged", "ruined",
        "ruling", "rural", "rustic", "ruthless", "sacred", "sad", "safe", "salient", "salty",
        "sane", "sarcastic", "satisfied", "satisfying", "savage", "scarce", "scared", "scary",
        "scenic", "scholarly", "scientific", "scornful", "scrap", "scratchy", "seamless",
        "seasonal", "secondary", "secret", "secretive", "secular", "secure", "sedentary",
        "selective", "selfish", "semantic", "senior", "sensational", "sensible", "sensitive",
        "sensory", "separate", "sequential", "serene", "serial", "serious", "servant",
        "severe", "shallow", "shameful", "shameless", "sharp", "sheer", "shiny", "shocked",
        "shocking", "short", "shrewd", "shrill", "shy", "sick", "significant", "silent",
        "silly", "similar", "simple", "simultaneous", "sincere", "single", "singular",
        "sinister", "skeptical", "skillful", "skinny", "slender", "slick", "slight", "slim",
        "slippery", "sloppy", "slow", "sluggish", "sly", "small", "smart", "smooth", "snug",
        "sober", "sociable", "social", "soft", "sole", "solemn", "solid", "solitary", "sore",
        "sorry", "sour", "southern", "spacious", "spare", "sparkling", "spatial", "special",
        "specific", "spectacular", "speedy", "spicy", "spiritual", "splendid", "spontaneous",
        "sporty", "spotless", "stable", "stale", "standard", "standing", "stark", "static",
        "statistical", "statutory", "steady", "steep", "sterile", "stern", "stiff", "still",
        "stingy", "stormy", "stout", "straight", "straightforward", "strange", "strategic",
        "strict", "striking", "stringent", "strong", "structural", "stubborn", "stunning",
        "stupid", "sturdy", "stylish", "subdued", "subjective", "sublime", "submissive",
        "subsequent", "substantial", "subtle", "suburban", "successful", "successive", "succinct",
        "sudden", "sufficient", "suitable", "sunny", "super", "superb", "superficial", "superior",
        "supernatural", "supple", "supporting", "supportive", "supreme", "sure", "surgical",
        "surprised", "surprising", "surreal", "susceptible", "suspicious", "sustainable",
        "sweet", "swift", "symmetric", "sympathetic", "systematic", "tactical", "talented",
        "tall", "tame", "tan", "tangible", "tart", "tasty", "taut", "technical", "technological",
        "tedious", "teenage", "temporary", "tenacious", "tender", "tense", "tentative",
        "terminal", "terrible", "terrific", "testy", "theoretical", "thermal", "thick",
        "thin", "thirsty", "thorough", "thoughtful", "thoughtless", "threatening", "thrifty",
        "thrilled", "thrilling", "thriving", "tidy", "tight", "timely", "timid", "tiny",
        "tired", "tiresome", "tiring", "tolerant", "top", "topical", "total", "tough", "toxic",
        "traditional", "tragic", "trailing", "trained", "trait", "tranquil", "transient",
        "transparent", "traumatic", "treacherous", "tremendous", "trendy", "tribute", "tricky",
        "trifling", "trim", "tripartite", "triumphant", "trivial", "troubled", "troublesome",
        "true", "trusting", "trustworthy", "truthful", "turbulent", "twin", "typical",
        "ubiquitous", "ugly", "ultimate", "unacceptable", "unanimous", "unavoidable", "unaware",
        "unbearable", "unbelievable", "uncertain", "unclear", "uncomfortable", "uncommon",
        "unconscious", "unconditional", "undeniable", "underlying", "understandable", "uneasy",
        "bald", "foggy", "icy", "sleepy", "smoggy", "turquoise", "violet", "middle-aged", "torn",
        "unemployed", "unequal", "uneven", "unexpected", "unfair", "unfamiliar", "unfavorable",
        "unfortunate", "unfriendly", "ungrateful", "unhappy", "uniform", "unilateral", "unique",
        "united", "universal", "unjust", "unknown", "unlawful", "unlikely", "unlimited",
        "unnecessary", "unofficial", "unpleasant", "unpopular", "unprecedented", "unpredictable",
        "unreasonable", "unreliable", "unresponsive", "unsafe", "unsatisfactory", "unseen",
        "unstable", "unsuccessful", "unsuitable", "unsure", "unthinkable", "untidy", "untrue",
        "unusual", "unwanted", "unwilling", "unwise", "unworthy", "upbeat", "upper", "upright",
        "upset", "urban", "urgent", "useful", "useless", "usual", "vacant", "vague", "vain",
        "valiant", "valid", "valuable", "variable", "varied", "various", "vast", "verbal",
        "versatile", "vertical", "viable", "vibrant", "vicious", "victorious", "vigilant",
        "vigorous", "vile", "violent", "virtual", "visible", "visionary", "vital", "vivid",
        "vocal", "vocational", "volatile", "voluntary", "vulnerable", "warm", "wary", "wasteful",
        "watchful", "waterproof", "wavy", "weak", "wealthy", "weary", "weekly", "weird",
        "welcome", "well", "western", "wet", "white", "whole", "wholesome", "wicked", "wide",
        "widespread", "wild", "willful", "willing", "windy", "wise", "witty", "wonderful",
        "wooden", "woolen", "wordy", "working", "workable", "world", "worldwide", "worn",
        "worried", "worrying", "worse", "worst", "worth", "worthless", "worthwhile", "worthy",
        "wounded", "wrathful", "wretched", "wrong", "wry", "yellow", "young", "youthful",
        "zealous"
    )

    // Common Adverbs
    private val commonAdverbs = setOf(
        "about", "above", "abroad", "absolutely", "accidentally", "accordingly", "accurately",
        "across", "actively", "actually", "adequately", "admittedly", "afresh", "after",
        "afterwards", "again", "ahead", "alike", "all", "almost", "alone", "along", "aloud",
        "already", "also", "altogether", "always", "angrily", "annually", "anxiously", "anyhow",
        "anymore", "anyway", "anywhere", "apart", "apparently", "appropriately", "approximately",
        "around", "aside", "automatically", "away", "back", "badly", "barely", "basically",
        "beautifully", "before", "beforehand", "behind", "below", "beside", "besides", "best",
        "better", "between", "beyond", "bitterly", "blindly", "boldly", "bravely", "briefly",
        "brightly", "broadly", "busily", "calmly", "carefully", "carelessly", "casually",
        "cautiously", "certainly", "cheaply", "cheerfully", "clearly", "cleverly", "close",
        "closely", "commonly", "comparatively", "completely", "confidently", "consequently",
        "considerably", "constantly", "continually", "correctly", "courageously", "crucially",
        "currently", "daily", "dangerously", "daringly", "darkly", "dearly", "decently",
        "decidedly", "decisively", "deeply", "definitely", "deliberately", "delicately",
        "delightfully", "densely", "desperately", "differently", "directly", "discreetly",
        "distinctly", "doubtfully", "down", "downstairs", "dramatically", "drastically",
        "duly", "eagerly", "early", "easily", "effectively", "efficiently", "effortlessly",
        "elegantly", "else", "elsewhere", "eminently", "emotionally", "emphatically", "endlessly",
        "energetically", "enormously", "enough", "entirely", "equally", "especially", "essentially",
        "even", "evenly", "eventually", "ever", "everywhere", "evidently", "exactly", "exceedingly",
        "excellently", "exceptionally", "excessively", "exclusively", "explicitly", "extremely",
        "fairly", "faithfully", "famously", "far", "fast", "fatally", "favorably", "ferociously",
        "fiercely", "finally", "financially", "finely", "firmly", "first", "firstly", "flatly",
        "fluently", "fondly", "foolishly", "forever", "formally", "formerly", "fortunately",
        "forward", "forwards", "frankly", "freely", "frequently", "freshly", "fully",
        "fundamentally", "furiously", "further", "furthermore", "generally", "generously",
        "gently", "genuinely", "gladly", "globally", "gloriously", "gradually", "gratefully",
        "greatly", "greedily", "grossly", "grudgingly", "guiltily", "happily", "hard", "hardly",
        "harshly", "hastily", "heartily", "heavily", "helplessly", "hence", "here", "heroically",
        "hesitantly", "highly", "hilariously", "historically", "honestly", "hopefully",
        "hopelessly", "horribly", "hourly", "how", "however", "hugely", "hurriedly", "ideally",
        "identically", "ill", "illegally", "immediately", "immensely", "impatiently",
        "imperfectly", "implicitly", "importantly", "impressively", "inadequately", "inappropriately",
        "incessantly", "incidentally", "increasingly", "incredibly", "indeed", "independently",
        "indirectly", "individually", "indoors", "inevitably", "infinitely", "informally",
        "inherently", "initially", "innocently", "insanely", "inside", "instantly", "instead",
        "instinctively", "insufficiently", "intelligently", "intensely", "intentionally",
        "intensely", "internally", "internationally", "intimately", "inwardly", "ironically",
        "irregularly", "irrevocably", "jealously", "jointly", "jovially", "joyfully", "just",
        "justly", "keenly", "kindly", "knowingly", "largely", "last", "lastly", "lately",
        "later", "latterly", "lavishly", "lazily", "least", "legally", "leisurely", "less",
        "lightly", "likely", "likewise", "literally", "little", "locally", "logically",
        "long", "loosely", "loudly", "lovingly", "low", "loyally", "luckily", "madly",
        "magically", "mainly", "manifestly", "manually", "markedly", "massively", "maybe",
        "meaningfully", "meantime", "meanwhile", "measurably", "mechanically", "medically",
        "mentally", "merely", "merrily", "methodically", "mightily", "mildly", "mindfully",
        "minimally", "miserably", "misleadingly", "mockingly", "moderately", "modestly",
        "momentarily", "monthly", "morally", "more", "moreover", "mostly", "much", "multiple",
        "mutually", "mysteriously", "naively", "narrowly", "naturally", "naughtily", "near",
        "nearly", "neatly", "necessarily", "negatively", "nervously", "never", "nevertheless",
        "newly", "next", "nicely", "nightly", "nobly", "noisily", "nominally", "nonetheless",
        "normally", "not", "notably", "noticeably", "now", "nowadays", "nowhere", "numbly",
        "obediently", "objectively", "obviously", "occasionally", "oddly", "off", "officially",
        "often", "okay", "once", "online", "only", "openly", "optimistically", "optionally",
        "orally", "ordinarily", "originally", "out", "outdoors", "outside", "outwardly",
        "over", "overboard", "overhead", "overnight", "overseas", "overwhelmingly", "painfully",
        "pardonably", "partially", "particularly", "partly", "passionately", "passively",
        "past", "patiently", "peacefully", "peculiarly", "perfectly", "periodically", "permanently",
        "personally", "persuasively", "physically", "picturesquely", "piteously", "plainly",
        "playfully", "pleasantly", "plentifully", "politely", "politically", "poorly",
        "popularly", "positively", "possibly", "potentially", "powerfully", "practically",
        "precisely", "predominantly", "prematurely", "presently", "presumably", "prettily",
        "pretty", "previously", "primarily", "principally", "privately", "probably", "professionally",
        "profoundly", "progressively", "prominently", "promptly", "properly", "proportionately",
        "proudly", "publicly", "punctually", "purely", "purposefully", "quaintly", "qualitatively",
        "quantitatively", "quarrelsomely", "quickly", "quietly", "quite", "quizzically",
        "radically", "randomly", "rapidly", "rarely", "rationally", "readily", "realistically",
        "really", "reasonably", "reassuringly", "recently", "recklessly", "regularly", "relentlessly",
        "reliably", "reluctantly", "remarkably", "remotely", "repeatedly", "reportedly",
        "reputedly", "resolutely", "respectfully", "respectively", "responsibly", "restfully",
        "restlessly", "retrospectively", "right", "rightfully", "rightly", "rigorously",
        "roughly", "round", "routinely", "rudely", "ruthlessly", "sadly", "safely", "scarcely",
        "scarily", "scientifically", "scornfully", "seamlessly", "seasonally", "secondly",
        "secretly", "securely", "seemingly", "seldom", "selectively", "selfishly", "sensibly",
        "sensitively", "separately", "sequentially", "serenely", "seriously", "severely",
        "sharply", "sheerly", "shortly", "shrilly", "shyly", "significantly", "silently",
        "similarly", "simply", "simultaneously", "sincerely", "singly", "singularly", "skillfully",
        "slightly", "slowly", "smartly", "smoothly", "so", "socially", "softly", "solely",
        "solemnly", "solidly", "solitarily", "someday", "somehow", "sometimes", "somewhat",
        "somewhere", "soon", "sorely", "sorry", "soundly", "sourly", "spatially", "specially",
        "specifically", "spectacularly", "speedily", "spiritually", "splendidly", "spontaneously",
        "sporadically", "stably", "starkly", "steadily", "steeply", "sternly", "stiffly",
        "still", "straight", "straightforwardly", "strangely", "strategically", "strictly",
        "strikingly", "stringently", "strongly", "stubbornly", "studiously", "stunningly",
        "subtly", "successfully", "successively", "succinctly", "suddenly", "sufficiently",
        "suitably", "superficially", "superlatively", "supposedly", "surely", "surprisingly",
        "suspiciously", "sweetly", "swiftly", "symmetrically", "sympathetically", "systematically",
        "temporarily", "tenderly", "tensely", "tentatively", "terribly", "terrificly",
        "theoretically", "there", "thereafter", "thereby", "therefore", "therein", "thickly",
        "thinly", "thoroughly", "thoughtfully", "thoughtlessly", "thus", "tightly", "timely",
        "timidly", "tirelessly", "together", "tomorrow", "tonight", "too", "totally", "touchingly",
        "traditionally", "tragically", "tranquilly", "transparently", "tremendously", "truly",
        "truthfully", "typically", "ultimately", "unanimously", "unavoidably", "unbearably",
        "unbelievably", "uncertainly", "unconditionally", "unconsciously", "undeniably",
        "underneath", "undoubtedly", "uneasily", "unequally", "unexpectedly", "unfairly",
        "unfortunately", "uniformly", "uniquely", "universally", "unjustly", "unknowingly",
        "unlawfully", "unnecessarily", "unquestionably", "unreasonably", "unsteadily",
        "unsuccessfully", "unusually", "unwillingly", "up", "upon", "upright", "upstairs",
        "upward", "upwards", "urgently", "usefully", "uselessly", "usually", "utterly",
        "vaguely", "vainly", "valiantly", "vastly", "verbally", "verily", "very", "vibrantly",
        "viciously", "victoriously", "vigorously", "violently", "virtually", "visibly",
        "visually", "vitally", "vividly", "vocally", "voluntarily", "warmly", "warily",
        "wastefully", "watchfully", "weakly", "weekly", "well", "west", "westward", "when",
        "where", "wherever", "wholly", "why", "wickedly", "widely", "wildly", "willfully",
        "willingly", "wisely", "wittily", "wonderfully", "worldwide", "worryingly", "wrongly",
        "yearly", "yesterday", "yet", "youthfully", "zealously"
    )

    fun lookup(rawWord: String): Set<LexicalCategory> {
        val word = rawWord.trim().lowercase(Locale.ROOT)
        if (word.isBlank()) return emptySet()

        val results = mutableSetOf<LexicalCategory>()

        if (word in properNounsAndCalendar) results += LexicalCategory.NOUN
        if (word in numberWords) results += LexicalCategory.NUMBER
        if (word in pronounWords) results += LexicalCategory.PRONOUN
        if (word in prepositionWords) results += LexicalCategory.PREPOSITION
        if (word in conjunctionWords) results += LexicalCategory.CONJUNCTION
        if (word in interjectionWords) results += LexicalCategory.INTERJECTION

        if (word in commonNouns) results += LexicalCategory.NOUN
        if (word in commonVerbs) results += LexicalCategory.VERB
        if (word in commonAdjectives) results += LexicalCategory.ADJECTIVE
        if (word in commonAdverbs) results += LexicalCategory.ADVERB

        // Check plural / inflection forms
        val baseNoun = lemmatizeNoun(word)
        if (baseNoun != null && (baseNoun in commonNouns || baseNoun in properNounsAndCalendar)) {
            results += LexicalCategory.NOUN
        }

        val baseVerb = lemmatizeVerb(word)
        if (baseVerb != null && baseVerb in commonVerbs) {
            results += LexicalCategory.VERB
        }

        val baseAdj = lemmatizeAdjective(word)
        if (baseAdj != null && baseAdj in commonAdjectives) {
            results += LexicalCategory.ADJECTIVE
        }

        return results
    }

    private fun lemmatizeNoun(word: String): String? {
        if (word.length <= 3) return null
        if (word.endsWith("ies") && word.length > 4) {
            return word.removeSuffix("ies") + "y"
        }
        if (word.endsWith("es") && word.length > 3) {
            val stem = word.removeSuffix("es")
            if (stem.endsWith("s") || stem.endsWith("sh") || stem.endsWith("ch") || stem.endsWith("x") || stem.endsWith("z") || stem.endsWith("o")) {
                return stem
            }
            return word.removeSuffix("s")
        }
        if (word.endsWith("s") && !word.endsWith("ss") && !word.endsWith("us") && !word.endsWith("is")) {
            return word.removeSuffix("s")
        }
        return null
    }

    private fun lemmatizeVerb(word: String): String? {
        if (word.length <= 3) return null
        if (word.endsWith("ing") && word.length > 4) {
            val stem = word.removeSuffix("ing")
            if (stem in commonVerbs) return stem
            if (stem + "e" in commonVerbs) return stem + "e"
            if (stem.length >= 3 && stem[stem.length - 1] == stem[stem.length - 2]) {
                val singleStem = stem.dropLast(1)
                if (singleStem in commonVerbs) return singleStem
            }
        }
        if (word.endsWith("ied") && word.length > 4) {
            val stem = word.removeSuffix("ied") + "y"
            if (stem in commonVerbs) return stem
        }
        if (word.endsWith("ed") && word.length > 4) {
            val stem = word.removeSuffix("ed")
            if (stem in commonVerbs) return stem
            if (stem + "e" in commonVerbs) return stem + "e"
            if (stem.length >= 3 && stem[stem.length - 1] == stem[stem.length - 2]) {
                val singleStem = stem.dropLast(1)
                if (singleStem in commonVerbs) return singleStem
            }
        }
        if (word.endsWith("s") && word.length > 3) {
            return lemmatizeNoun(word) // same rules as -s/-es
        }
        return null
    }

    private fun lemmatizeAdjective(word: String): String? {
        if (word.length <= 4) return null
        if (word.endsWith("est") && word.length > 5) {
            val stem = word.removeSuffix("est")
            if (stem in commonAdjectives) return stem
            if (stem + "e" in commonAdjectives) return stem + "e"
        }
        if (word.endsWith("er") && word.length > 4) {
            val stem = word.removeSuffix("er")
            if (stem in commonAdjectives) return stem
            if (stem + "e" in commonAdjectives) return stem + "e"
        }
        return null
    }
}
