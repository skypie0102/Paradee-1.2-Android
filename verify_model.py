#!/usr/bin/env python3
"""Host smoke test: wrapper equivalence and sherpa inference for all voices."""
import json
import pathlib
import tempfile
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
    cfg = sherpa.GenerationConfig(); cfg.sid = sid; cfg.silence_scale = 1.0; cfg.phoneme_codepoints = [[ord(c) for c in 'həlˈoʊ.']]
    audio = tts.generate('', cfg)
    assert audio.sample_rate == 24000 and len(audio.samples) > 0 and np.isfinite(audio.samples).all()
    print(f'voice {sid}: sherpa generated {len(audio.samples)} samples')

# Compare sherpa's rendered length with the original graph for a sentence
# containing a pause. The default 0.2 scale shortens it; 1.0 preserves it.
phonemes = 'həlˈO, hˌW ɑɹ ju dˌuɪŋ tədˈA?'
vocab = json.loads((root / 'model/config.json').read_text())['vocab']
ids = np.array([[0] + [vocab[c] for c in phonemes if c in vocab] + [0]], dtype=np.int64)
raw = onnx.load(original)
wrapped_model = onnx.load(wrapped)
for model in (raw, wrapped_model):
    for node in model.graph.node:
        if node.op_type == 'RandomNormalLike':
            node.attribute.append(onnx.helper.make_attribute('seed', 123.0))
options = ort.SessionOptions(); options.intra_op_num_threads = 1
reference = ort.InferenceSession(raw.SerializeToString(), options, providers=['CPUExecutionProvider'])
reference_audio = reference.run(None, {'input_ids': ids, 'speed': np.array([1.0], np.float32), 'voice': np.array([0], np.int64)})[0][0]
with tempfile.TemporaryDirectory() as tmp:
    model_path = pathlib.Path(tmp) / 'seeded.onnx'
    onnx.save(wrapped_model, model_path)
    lengths = {}
    for scale in (0.01, 0.2, 0.5, 0.9, 1.0):
        kokoro = sherpa.OfflineTtsKokoroModelConfig(model=str(model_path), voices=str(p/'voices.bin'), tokens=str(p/'tokens.txt'))
        engine = sherpa.OfflineTts(sherpa.OfflineTtsConfig(model=sherpa.OfflineTtsModelConfig(kokoro=kokoro, num_threads=1)))
        gen = sherpa.GenerationConfig(); gen.sid = 0; gen.speed = 1.0; gen.silence_scale = scale
        gen.phoneme_codepoints = [[ord(c) for c in phonemes]]
        lengths[scale] = len(engine.generate('', gen).samples)
    assert lengths[1.0] == len(reference_audio), (lengths, len(reference_audio))
    assert lengths[0.01] < lengths[0.2] < lengths[0.5] < lengths[0.9] < lengths[1.0], lengths
    print(f"pause regression (samples by retained scale): {lengths}")
