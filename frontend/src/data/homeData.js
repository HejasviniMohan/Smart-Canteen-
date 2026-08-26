export const COMBOS = [
  { id: 'c1', name: 'South Indian Breakfast',  emoji: '🥞', includes: 'Idli + Dosa + Vada + Coffee',  mrp: 180, price: 149, save: 31 },
  { id: 'c2', name: 'Student Lunch Box',     emoji: '🍱', includes: 'Rice + Curry + Roti + Salad',   mrp: 220, price: 179, save: 41 },
  { id: 'c3', name: 'Snack Attack',           emoji: '🥪', includes: 'Sandwich + Samosa + Juice',     mrp: 150, price: 119, save: 31 },
  { id: 'c4', name: 'Chai & Chat Combo',      emoji: '☕', includes: 'Masala Chai + Biscuits + Samosa', mrp: 90,  price: 69,  save: 21 },
  { id: 'c5', name: 'Chinese Bowl',           emoji: '🥡', includes: 'Noodles + Manchurian + Soup',    mrp: 260, price: 209, save: 51 },
  { id: 'c6', name: 'Sweet Tooth',            emoji: '🍨', includes: 'Gulab Jamun + Ice Cream + Coffee', mrp: 170, price: 139, save: 31 },
]

export const TESTIMONIALS = [
  { id: 1, name: 'Aarav Sharma',  dept: 'CSE, 3rd Year',    rating: 5, text: "Honestly, the token system is a lifesaver. I used to skip lunch because the queue was 25 minutes long. Now I order from class and pick it up on the way. Game changer." },
  { id: 2, name: 'Priya Iyer',    dept: 'ECE, 2nd Year',    rating: 5, text: "The live order tracking is so satisfying. Watching my food go from placed → preparing → ready while I finish my lab is genuinely fun." },
  { id: 3, name: 'Rohan Verma',   dept: 'Mechanical, 4th Yr', rating: 4, text: "UPI payment + digital receipt is super clean. Best part — no more shouting my order across the counter." },
  { id: 4, name: 'Sneha Kapoor',  dept: 'Biotech, 1st Year', rating: 5, text: "Loved the Today's Special feature. Found a butter chicken fried rice I didn't even know existed, and it was only ₹75." },
  { id: 5, name: 'Aditya Rao',    dept: 'MBA, 2nd Year',    rating: 4, text: "The combo meals are actually value for money. ₹149 for a full breakfast is unbeatable." },
  { id: 6, name: 'Meera Nair',    dept: 'Civil, 3rd Year',  rating: 5, text: "Finally something that feels like a real product, not a college project. The UI is on par with Swiggy." },
]

export const FAQS = [
  {
    q: 'How does the digital token work?',
    a: 'Once you place and pay for an order, the system instantly generates a unique token (e.g. A127) along with a QR code. Show this at the counter — staff scans it, marks your order ready, and hands it over. No confusion, no waiting in line.',
  },
  {
    q: 'Can I cancel an order after placing it?',
    a: 'Yes, you can cancel any order that is still in PLACED or ACCEPTED status. Once the kitchen starts PREPARING, the order is non-cancellable. Refunds for cancelled prepaid orders are processed to your original payment method within 3-5 business days.',
  },
  {
    q: 'Which payment methods are accepted?',
    a: 'We support UPI (GPay, PhonePe, Paytm), credit/debit cards, net banking, and Smart Canteen wallet. All transactions are secured by Razorpay with end-to-end encryption.',
  },
  {
    q: 'How do I track my order in real time?',
    a: 'After placing an order, head to "My Orders" and tap your active order. You\'ll see live status updates — Placed → Accepted → Preparing → Ready for Pickup. We also send push notifications at every stage so you never miss a beat.',
  },
  {
    q: 'Can I order for a specific pickup time?',
    a: 'Yes! While placing an order you can choose a scheduled pickup slot. We recommend ordering at least 10 minutes before your desired pickup time to allow for preparation.',
  },
  {
    q: 'Is there a minimum order amount?',
    a: 'There is no minimum order value. However, a small platform fee of ₹2 is applied to orders below ₹50 to keep the service sustainable.',
  },
]

export const FEATURES = [
  { title: 'Skip Long Queues',         desc: 'Order from anywhere on campus and pick up when ready. No more 20-minute waits between classes.', icon: 'zap',     from: '#F97316', to: '#FB923C' },
  { title: '100% Cashless Payments',   desc: 'Pay securely via UPI, cards, or wallet. No cash handling, no change issues.',                icon: 'shield',  from: '#10B981', to: '#34D399' },
  { title: 'Live Order Notifications', desc: 'Get instant push updates when your order is accepted, preparing, or ready for pickup.',    icon: 'bell',    from: '#3B82F6', to: '#60A5FA' },
  { title: 'Faster Food Pickup',       desc: 'Average pickup time reduced to under 5 minutes once your token is up.',                       icon: 'rocket',  from: '#8B5CF6', to: '#A78BFA' },
]
