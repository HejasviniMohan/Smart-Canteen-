import { useState } from 'react'
import { ChevronDown } from 'lucide-react'
import { motion, AnimatePresence } from 'framer-motion'
import { FAQS } from '../data/homeData'

export default function FAQ() {
  const [open, setOpen] = useState(0)
  return (
    <div className="sc-faq">
      {FAQS.map((f, idx) => (
        <motion.div
          key={idx}
          className={`sc-faq-item ${open === idx ? 'open' : ''}`}
          initial={{ opacity: 0, y: 16 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: '-30px' }}
          transition={{ duration: 0.35, delay: idx * 0.04 }}
        >
          <button onClick={() => setOpen(open === idx ? -1 : idx)} aria-expanded={open === idx}>
            <span>{f.q}</span>
            <span className="ic"><ChevronDown size={20} /></span>
          </button>
          <AnimatePresence initial={false}>
            {open === idx && (
              <motion.div
                initial={{ height: 0, opacity: 0 }}
                animate={{ height: 'auto', opacity: 1 }}
                exit={{ height: 0, opacity: 0 }}
                transition={{ duration: 0.25 }}
                style={{ overflow: 'hidden' }}
              >
                <div style={{ padding: '0 22px 18px', color: 'var(--muted)', lineHeight: 1.6 }}>{f.a}</div>
              </motion.div>
            )}
          </AnimatePresence>
        </motion.div>
      ))}
    </div>
  )
}
