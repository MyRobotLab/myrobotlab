class Listener:
    def __init__(self, topic_method: str, callback_name: str, callback_method: str):
        self.topic_method = topic_method
        self.callback_name = callback_name
        self.callback_method = callback_method
