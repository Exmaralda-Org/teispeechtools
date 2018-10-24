---
title: Simple EXMARALDA to TEI:
author: BF
---

# Construct

- for every line, make an `annotationBlock{@who <= //person/@xml:id, @start}/u`,
    - `persName/{forename, lastname}`?
    - (split heuristically, if >= 2 segments)
    - check duplicates? (in person header class)
    - action as `incident/desc`
    - info as `spanGrp{@type="info"}` without `<span>`s? or `span{@from=u/@start 
      @to=u/@end}`?
    - `anchors{@sync=timeline/when/@xml:id}` for overlaps:
- the `timeline{@unit, @origin}/when{@xml:id, @interval??, @since??}` contains
    - `@start`s/`@end`s of utterances
    - intermediate marks for overlaps
- speaker catalogue at `/profileDesc/particDesc/person{@xml:id, @n, @sex??}`
    - generate `persName/abbr == person/@id == person/@n` from name


# Questions

- In simple format, what's obligatory?
- is `<u>` with speaker and such obsolete? (FOLK documents are with `<annotationBlock>`)
- Why does the TEI schema only use `<forename>`: ‘optimistic’ heuristics?
- Can `person/@n`
- Should 'normalized' `<SpanGrp>`s be moved to `w/@norm`, or be respected in (re)normalization? 
  (That presupposes that the label of said `<SpanGrp>`s can be determined, and their 
  segmentation level is adequate.)
- Is `persName/abbr == person/@n`?
- Do we need `<synch>` at the beginning of `<u>`s, or does `@start` `@end` suffice? 
  In any case, we need anchors for `<incident>`s.


# Algorithm / Procedure

- Either use stacks for utterances and timeline, `LinkedHashMap` [tag => object] 
  for participants
- or walk tree thrice.
