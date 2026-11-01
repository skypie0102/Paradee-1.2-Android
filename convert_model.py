#!/usr/bin/env python3
"""Adapt Paradee 1.2's voice ID input to sherpa-onnx 2.0.1 Kokoro's style input."""
import json, pathlib, struct
import onnx
from onnx import helper, TensorProto

root = pathlib.Path(__file__).resolve().parent
src = root / 'model'
out = root / 'app' / 'src' / 'main' / 'assets' / 'paradee'
out.mkdir(parents=True, exist_ok=True)
cfg = json.loads((src / 'config.json').read_text())
assert cfg['version'] == '1.2' and len(cfg['voices']) == 4
model = onnx.load(src / 'paradee_int8.onnx')
g = model.graph
inputs = {x.name: x for x in g.input}
assert list(inputs) == ['input_ids', 'speed', 'voice']
del g.input[:]
g.input.extend([inputs['input_ids'], helper.make_tensor_value_info('style', TensorProto.FLOAT, [1, 1]), inputs['speed']])
g.initializer.append(helper.make_tensor('paradee_voice_shape', TensorProto.INT64, [1], [1]))
g.node.insert(0, helper.make_node('Reshape', ['paradee_voice_float', 'paradee_voice_shape'], ['paradee_voice_flat']))
g.node.insert(0, helper.make_node('Cast', ['style'], ['paradee_voice_float'], to=TensorProto.FLOAT))
g.node.insert(2, helper.make_node('Cast', ['paradee_voice_flat'], ['voice'], to=TensorProto.INT64))
metadata = {'sample_rate':'24000', 'version':'1', 'n_speakers':'4', 'style_dim':'512,1,1', 'voice':'en-us', 'speaker_names':','.join(cfg['voices'])}
for k,v in metadata.items():
    entry=model.metadata_props.add(); entry.key=k; entry.value=v
onnx.checker.check_model(model)
onnx.save(model, out / 'model.onnx')
(out / 'voices.bin').write_bytes(b''.join(struct.pack('<f', float(sid)) * 512 for sid in range(4)))
with (out / 'tokens.txt').open('w', encoding='utf-8') as f:
    for c,i in cfg['vocab'].items():
        f.write(f'{c} {i}\n' if c != ' ' else f'{i}\n')
print('Wrote', out)
