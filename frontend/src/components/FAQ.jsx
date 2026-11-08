import { useState } from 'react'
import { Plus } from 'lucide-react'
import { motion, AnimatePresence, useReducedMotion } from 'framer-motion'
import { FAQS } from '../data/homeData'

export default function FAQ() {
  const [open, setOpen] = useState(0)
  const reduce = useReducedMotion()
  return (
    <div className="sc-faq">
      {FAQS.map((f, idx) => {
        const isOpen = open === idx
        return (
          <div key={f.q} className={`sc-faq-item ${isOpen ? 'open' : ''}`}>
            <button type="button" onClick={() => setOpen(isOpen ? -1 : idx)} aria-expanded={isOpen} aria-controls={`faq-${idx}`}>
              <span>{f.q}</span>
              <span className="ic" aria-hidden="true"><Plus size={20} /></span>
            </button>
            <AnimatePresence initial={false}>
              {isOpen && (
                <motion.div
                  id={`faq-${idx}`}
                  initial={{ height: 0, opacity: 0 }}
                  animate={{ height: 'auto', opacity: 1 }}
                  exit={{ height: 0, opacity: 0 }}
                  transition={reduce ? { duration: 0 } : { duration: 0.25, ease: [0.23, 1, 0.32, 1] }}
                  style={{ overflow: 'hidden' }}
                >
                  <p className="answer">{f.a}</p>
                </motion.div>
              )}
            </AnimatePresence>
          </div>
        )
      })}
    </div>
  )
}
