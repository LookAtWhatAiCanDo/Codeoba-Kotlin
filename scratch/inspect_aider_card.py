from PIL import Image

img_path = "/Users/pv/.gemini/antigravity/brain/c45d186f-ac5b-47a5-b59d-52c5a3eb368f/media__1779388854642.png"
try:
    img = Image.open(img_path)
    w, h = img.size
    
    # We want to crop the Aider card. Let's find where the text "Status: Not Detected" starts.
    # We can search for the text or simply crop the bottom part of the settings dialog.
    # The settings dialog is from y=100 to y=580.
    # The Aider card is roughly at y=450 to 570, x=500 to 975.
    aider_card = img.crop((490, 440, 975, 570))
    aider_card.save("/Users/pv/.gemini/antigravity/brain/c45d186f-ac5b-47a5-b59d-52c5a3eb368f/aider_card.png")
    print("Aider card cropped and saved to aider_card.png")
    
    # Let's inspect the switch location in the aider card.
    # The switch should be on the right side, around x=880 to 950 relative to the dialog,
    # which is around x=380 to 450 in the cropped aider_card.
    # Let's find any colored pixels in the right side of the aider card.
    cyan_count = 0
    grey_count = 0
    
    # AccentCyan is roughly (104, 225, 252)
    # SlateSurface (unchecked track) is around (20, 20, 26) -> #14141a
    # Let's print the colors in the right half of the card.
    width, height = aider_card.size
    for y in range(height):
        for x in range(int(width * 0.8), width):
            r, g, b, *a = aider_card.getpixel((x, y))
            dist_cyan = ((r - 104)**2 + (g - 225)**2 + (b - 252)**2)**0.5
            dist_grey = ((r - 20)**2 + (g - 20)**2 + (b - 26)**2)**0.5
            if dist_cyan < 30:
                cyan_count += 1
            if dist_grey < 15:
                grey_count += 1
                
    print(f"Right-side pixel counts in Aider card: Cyan={cyan_count}, Grey={grey_count}")
except Exception as e:
    print(f"Error: {e}")
