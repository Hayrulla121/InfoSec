import { useEffect, useState } from 'react';

/**
 * The system name, typed out as if into a terminal, with a block caret that
 * goes on blinking at the end of the line forever.
 *
 * The typing runs once per string. Retyping on every render would restart the
 * animation whenever anything else on the screen changed - a keystroke in the
 * login field, say - which is why it keys on the text and nothing else.
 * Switching language deliberately DOES retype: it is a different name.
 *
 * Accessibility: the heading carries the finished string as its accessible
 * name, so a screen reader announces the whole thing once rather than following
 * the characters in. The caret is decorative and hidden.
 */

/** Milliseconds between characters. ~57 chars lands a little under two seconds. */
const STEP_MS = 32;

function prefersReducedMotion(): boolean {
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches;
}

export function TerminalTitle({ text }: { text: string }) {
  const [shown, setShown] = useState('');
  const [typedFor, setTypedFor] = useState<string | null>(null);

  /*
   * Reset DURING render rather than in an effect.
   *
   * An effect runs after the browser has already been given a frame, and in
   * that frame `shown` still holds the previous name while the reserved tail
   * has been recomputed against the new one. The two disagree for exactly one
   * frame, and the card visibly snaps to the wrong height and back on every
   * language switch. Setting state during render of the same component is the
   * supported way to adjust to a changed prop: React discards the in-progress
   * output and re-renders before anything reaches the screen.
   */
  if (typedFor !== text) {
    setTypedFor(text);
    // No typing when the user asked for less motion - the name is information,
    // the animation is decoration, and only one of those is negotiable.
    setShown(prefersReducedMotion() ? text : '');
  }

  useEffect(() => {
    if (prefersReducedMotion()) return;

    let i = 0;
    const id = window.setInterval(() => {
      i += 1;
      // slice() rather than concatenation: at any tick the state is a prefix of
      // the real string, so a dropped or doubled tick cannot corrupt the text.
      setShown(text.slice(0, i));
      if (i >= text.length) window.clearInterval(id);
    }, STEP_MS);

    return () => window.clearInterval(id);
  }, [text]);

  return (
    <h1 className="login-title" aria-label={text}>
      <span aria-hidden="true">{shown}</span>
      <span className="login-caret" aria-hidden="true" />
      {/* The untyped remainder, held invisible. It reserves exactly the space
          the finished name will occupy, so the card does not grow line by line
          as the text arrives - and it does so for whichever language is
          selected, which a fixed min-height could only approximate. */}
      <span className="login-title-rest" aria-hidden="true">
        {text.slice(shown.length)}
      </span>
    </h1>
  );
}
