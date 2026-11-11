use misaki_rs::{G2P, Language};

fn check_fixture(raw: &str, language: Language) {
    let fixture: serde_json::Value = serde_json::from_str(raw).unwrap();
    assert_eq!(fixture["misaki_version"], "0.9.4");
    let mut g2p = G2P::with_fallback(language, None);
    g2p.unk.clear();
    let cases = fixture["cases"].as_array().unwrap();
    for case in cases {
        let text = case["text"].as_str().unwrap();
        let expected = case["ps"].as_str().unwrap();
        let (actual, _) = g2p.g2p(text).unwrap();
        assert_eq!(actual, expected, "Misaki mismatch for: {text:?}");
    }
    println!("Matched {} Python Misaki fixtures", cases.len());
}

#[test]
fn python_misaki_us_adversarial_corpus() {
    check_fixture(include_str!("fixtures/misaki-0.9.4-adversarial-us.json"), Language::EnglishUS);
}

#[test]
fn python_misaki_british_corpus() {
    check_fixture(include_str!("fixtures/misaki-0.9.4-gb.json"), Language::EnglishGB);
}
