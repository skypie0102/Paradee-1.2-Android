#!/usr/bin/env python3
"""Host smoke test: wrapper equivalence and sherpa inference for all voices."""
import pathlib
import numpy as np
import onnx
import onnxruntime as ort
import sherpa_onnx as sherpa
root = pathlib.Path(__file__).resolve().parent
original = root / 'model/paradee_int8.onnx'
wrapped = root / 'app/src/main/assets/paradee/model.onnx'
models = []
for path in (original, wrapped):
    model = onnx.load(path)
    for node in model.graph.node:
        if node.op_type == 'RandomNormalLike':
            node.attribute.append(onnx.helper.make_attribute('seed', 123.0))
    models.append(ort.InferenceSession(model.SerializeToString(), providers=['CPUExecutionProvider']))
x = np.array([[0, 50, 83, 54, 57, 4, 0]], np.int64)
for sid in range(4):
    args = {'input_ids': x, 'speed': np.array([1.0], np.float32)}
    actual = models[0].run(None, dict(args, voice=np.array([sid], np.int64)))[0]
    adapted = models[1].run(None, dict(args, style=np.array([[sid]], np.float32)))[0]
    np.testing.assert_array_equal(actual, adapted)
    print(f'voice {sid}: ONNX outputs match exactly ({adapted.size} samples)')
p = root / 'app/src/main/assets/paradee'
config = sherpa.OfflineTtsConfig(model=sherpa.OfflineTtsModelConfig(kokoro=sherpa.OfflineTtsKokoroModelConfig(model=str(p/'model.onnx'), voices=str(p/'voices.bin'), tokens=str(p/'tokens.txt')), num_threads=1))
tts = sherpa.OfflineTts(config)
assert tts.sample_rate == 24000 and tts.num_speakers == 4
for sid in range(4):
    cfg = sherpa.GenerationConfig(); cfg.sid = sid; cfg.phoneme_codepoints = [[ord(c) for c in 'həlˈoʊ.']]
    audio = tts.generate('', cfg)
    assert audio.sample_rate == 24000 and len(audio.samples) > 0 and np.isfinite(audio.samples).all()
    print(f'voice {sid}: sherpa generated {len(audio.samples)} samples')
