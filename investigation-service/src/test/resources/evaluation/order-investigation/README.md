# Order Investigation Evaluation Dataset

This directory contains versioned evaluation datasets for order investigation
explanations.

`dataset-v1.json` is the initial human-reviewed baseline. Each case provides:

- the authoritative investigation context supplied to the AI adapter
- the structured fields expected from a grounded response
- facts that a useful explanation must communicate
- claims that the explanation must not make

The dataset does not prescribe an exact explanation sentence. Different
wording is acceptable when it preserves the expected facts and avoids the
forbidden claims.

Dataset, prompt and model versions are independent. The same dataset can
compare multiple prompt or model configurations. Evaluation results record the
exact dataset, prompt and model combination used for a run. A semantic change
to cases or success expectations creates a new dataset version; corrections
that do not alter the meaning may be applied to the current version.

The cases contain synthetic order data only. Production prompts, responses and
customer information must not be copied into this dataset.
