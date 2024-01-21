import random


class CodecUtil:
    # put in codec
    @staticmethod
    def generate_name():
        adjectives = [
            "mechanical",
            "electronic",
            "cybernetic",
            "autonomous",
            "programmable",
        ]
        nouns = ["android", "cyborg", "robo", "droid", "bot"]
        adj = random.choice(adjectives)
        noun = random.choice(nouns)

        return f"{adj}-{noun}"
