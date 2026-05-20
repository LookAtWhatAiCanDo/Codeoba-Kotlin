from PIL import Image

img_path = "/Users/pv/.gemini/antigravity/brain/c45d186f-ac5b-47a5-b59d-52c5a3eb368f/aider_status_crop.png"
try:
    img = Image.open(img_path)
    img.thumbnail((120, 80))
    w, h = img.size
    
    cyan_target = (104, 225, 252) # The BGR-like cyan we found
    
    for y in range(h):
        line = ""
        for x in range(w):
            pixel = img.getpixel((x, y))
            r, g, b = pixel[0], pixel[1], pixel[2]
            # Distance to cyan
            dist_cyan = ((r - cyan_target[0])**2 + (g - cyan_target[1])**2 + (b - cyan_target[2])**2)**0.5
            brightness = (r + g + b) / 3
            
            if dist_cyan < 30:
                line += "@"
            elif brightness > 200:
                line += "#"
            elif brightness > 100:
                line += "*"
            elif brightness > 50:
                line += "."
            else:
                line += " "
        print(line)
except Exception as e:
    print(f"Error: {e}")
