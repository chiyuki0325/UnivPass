import json

# 遵循《一言》的 AGPL 开源协议，将此脚本和 Universal Pass 源码一同发布

sentences = []

for i in ["a", "b", "c", "e", "h", "i", "k", "j"]:
    with open("sentences/" + i + ".json", "r") as f:
        data = json.load(f)
        for s in data:
            f = s["from"]
            if s["from_who"] is not None:
                f = s["from_who"] + ", " + f
            sentences.append([s["hitokoto"], f])

with open("hitokoto_sentences.json", "w") as f:
    json.dump(sentences, f, ensure_ascii=False, separators=(",", ":"))

print("Length:", len(sentences))
