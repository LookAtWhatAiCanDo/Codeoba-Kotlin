from PIL import Image

img_path = "/Users/pv/.gemini/antigravity/brain/c45d186f-ac5b-47a5-b59d-52c5a3eb368f/media__1779388854642.png"
try:
    img = Image.open(img_path)
    w, h = img.size
    rgb_img = img.convert('RGB')
    
    cyan_target = (104, 225, 252) # The scaled cyan color we found
    matching_pixels = []
    
    for y in range(h):
        for x in range(w):
            r, g, b = rgb_img.getpixel((x, y))
            # Check if very close to the target color
            if abs(r - cyan_target[0]) < 5 and abs(g - cyan_target[1]) < 5 and abs(b - cyan_target[2]) < 5:
                matching_pixels.append((x, y))
                
    print(f"Found {len(matching_pixels)} cyan pixels.")
    # Print distinct clusters by Y coordinate
    if matching_pixels:
        # Group by Y ranges (differences > 10 start a new group)
        matching_pixels.sort(key=lambda p: p[1])
        groups = []
        current_group = [matching_pixels[0]]
        for p in matching_pixels[1:]:
            if p[1] - current_group[-1][1] <= 10:
                current_group.append(p)
            else:
                groups.append(current_group)
                current_group = [p]
        groups.append(current_group)
        
        print(f"Detected {len(groups)} distinct Y-coordinate clusters:")
        for idx, g in enumerate(groups):
            ys = [p[1] for p in g]
            xs = [p[0] for p in g]
            min_y, max_y = min(ys), max(ys)
            min_x, max_x = min(xs), max(xs)
            print(f"  Cluster {idx+1}: X range [{min_x}, {max_x}], Y range [{min_y}, {max_y}], pixel count: {len(g)}")
except Exception as e:
    print(f"Error: {e}")
