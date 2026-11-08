// These really are steps in order, so they are numbered.
const STEPS = [
  { title: 'Browse the menu', desc: 'See what is available today, including specials and combos.' },
  { title: 'Pick a slot', desc: 'Choose a pickup time, then pay online or in cash at pickup.' },
  { title: 'Track it live', desc: 'Get a token and QR code, and watch your order move through the kitchen.' },
  { title: 'Scan and collect', desc: 'Staff scan your QR code at the counter and hand over your food.' },
]

export default function HowItWorks() {
  return (
    <ol className="sc-how">
      {STEPS.map((s, idx) => (
        <li key={s.title} className="step">
          <span className="step-num" aria-hidden="true">{idx + 1}</span>
          <h3>{s.title}</h3>
          <p>{s.desc}</p>
        </li>
      ))}
    </ol>
  )
}
