import os

def read_varint(data, offset):
    result = 0
    shift = 0
    while offset < len(data):
        b = data[offset]
        offset += 1
        result |= (b & 0x7f) << shift
        if not (b & 0x80):
            return result, offset
        shift += 7
    return result, offset

def parse_to_dict(data, start, end):
    res = {}
    offset = start
    while offset < end:
        tag, offset = read_varint(data, offset)
        wire_type = tag & 0x07
        field_num = tag >> 3
        
        if wire_type == 0:  # Varint
            val, offset = read_varint(data, offset)
            res[field_num] = val
        elif wire_type == 1:  # 64-bit
            val = int.from_bytes(data[offset:offset+8], 'little')
            offset += 8
            res[field_num] = val
        elif wire_type == 2:  # Length-delimited
            length, offset = read_varint(data, offset)
            val_bytes = data[offset:offset+length]
            offset += length
            res[field_num] = val_bytes
        elif wire_type == 5:  # 32-bit
            val = int.from_bytes(data[offset:offset+4], 'little')
            offset += 4
            res[field_num] = val
        else:
            break
    return res

pb_path = os.path.expanduser("~/.gemini/antigravity/agyhub_summaries_proto.pb")
with open(pb_path, 'rb') as f:
    data = f.read()

offset = 0
while offset < len(data):
    tag, offset = read_varint(data, offset)
    wire_type = tag & 0x07
    field_num = tag >> 3
    if field_num == 1 and wire_type == 2:
        length, offset = read_varint(data, offset)
        entry_bytes = data[offset:offset+length]
        offset += length
        
        entry = parse_to_dict(entry_bytes, 0, len(entry_bytes))
        uuid_bytes = entry.get(1, b"")
        uuid = uuid_bytes.decode('utf-8', errors='ignore')
        
        info_bytes = entry.get(2, b"")
        if info_bytes:
            info = parse_to_dict(info_bytes, 0, len(info_bytes))
            title_bytes = info.get(1, b"")
            title = title_bytes.decode('utf-8', errors='ignore') if title_bytes else "Untitled"
            # Parse field 1 of title_bytes if it was parsed as sub-message
            if title_bytes and (title_bytes[0] & 0x07) == 2:
                # Try parsing nested
                try:
                    title_info = parse_to_dict(title_bytes, 0, len(title_bytes))
                    title = title_info.get(1, b"").decode('utf-8', errors='ignore')
                except Exception:
                    pass
            
            f16 = info.get(16, None)
            f21 = info.get(21, None)
            f22 = info.get(22, None)
            
            # Let's print out the summary
            print(f"UUID: {uuid}")
            print(f"  Title: {title.strip()}")
            print(f"  Field 16: {f16}, Field 21: {f21}, Field 22: {f22}")
            # print all fields in info
            keys = sorted(list(info.keys()))
            info_fields = []
            for k in keys:
                v = info[k]
                if not isinstance(v, bytes):
                    info_fields.append(f"{k}:{v}")
            print(f"  Info Numeric Fields: {', '.join(info_fields)}")
            print()
