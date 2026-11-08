import { useReveal } from '../hooks/useReveal'

/** Wraps content that eases in on first scroll. `stagger` brings its children in one after another. */
export default function Reveal({ as: Tag = 'div', stagger = false, className = '', children, ...rest }) {
  const ref = useReveal()
  return (
    <Tag ref={ref} data-reveal="" className={`${stagger ? 'stagger ' : ''}${className}`.trim()} {...rest}>
      {children}
    </Tag>
  )
}
