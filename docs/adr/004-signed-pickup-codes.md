# ADR 4: Signed, single-use QR pickup codes

**Context.** The QR contained `SMARTCANTEEN|TOKEN:A101|ORDER:5`, which anyone could type into a QR generator to claim someone else's food. Nothing stopped an order being collected twice.

**Threats and defences.**
| Threat | Defence |
|---|---|
| Forging a code | `SC1.orderId.nonce.signature`, signed with HMAC-SHA256 using a key only the server holds |
| Changing a real code to another order | The signature covers the order id |
| Reusing a code, or a screenshot | Collection succeeds only from READY, and then the order is COMPLETED |
| Two simultaneous scans | One atomic `UPDATE ... WHERE status='READY' AND payment_status='PAID'`. Tested with 10 concurrent requests: exactly 1 succeeds |
| Staff or other users reading codes | The code is sent only to the order's owner |

**Why an atomic UPDATE?** Check-then-update has a gap between the two steps where two requests can both pass the check. With one statement, the database decides.

**Code length.** The signature is HMAC-SHA256 truncated to 128 bits (a standard truncation, still infeasible to forge), and the nonce is 64 random bits. The first version used the full 256-bit signature and a 128-bit nonce, which made an 84-character payload and a dense 45×45 QR code. At around 46 characters the QR has fewer, larger modules, which is easier for any camera.

**Scanning reliability.** In camera tests about 1 scan in 25 failed. Shortening the code did not fix this. The cause was ZXing's JavaScript decoder: on some full-size, sharp frames it finds nothing, although zbar reads the same frame and ZXing itself reads it once scaled down. The scanner now tries each frame at several sizes (full, ½, ¾, ⅓); each attempt takes milliseconds.

**Trade-offs.** If a student's screenshot is shared, whoever reaches the counter first can collect. Staff see the student's name to help catch this. Manual token entry stays as a fallback for broken cameras, which relies on staff checking the name.
