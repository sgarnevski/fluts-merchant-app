import type { components } from '../api/schema.gen'
import styles from './Pile.module.css'

type SchuurResponse = components['schemas']['SchuurResponse']

/** A flut sells for 10 florins in Holland. */
const SELLING_PRICE = 10

/** Piles are drawn at most this tall; the rest is in the downloadable file. */
export const MAX_SHOWN_BOXES = 20

function boxClass(price: number, bought: boolean): string {
  const profit = price < SELLING_PRICE ? styles.profit : price > SELLING_PRICE ? styles.loss : styles.even
  return bought ? `${styles.box} ${profit} ${styles.bought}` : `${styles.box} ${profit}`
}

/**
 * One schuur drawn as a stack of boxes, top box first, coloured by profit or loss per flut.
 * The boxes to buy (the smallest optimal count, from the top) are outlined, and the caption says
 * what the best buy at this schuur earns. Only the top {@link MAX_SHOWN_BOXES} boxes are drawn.
 */
export function Pile({
  title,
  schuur,
  fileUrl,
  resultUrl,
}: {
  readonly title: string
  readonly schuur: SchuurResponse
  /** Downloads this schuur as a text file in the input format of the specification. */
  readonly fileUrl: string
  /** Downloads this schuur's result in the output format of the specification. */
  readonly resultUrl: string
}) {
  const [buy, ...alternatives] = schuur.flutCounts
  const summary = buy === 0 ? 'Buy nothing' : `Buy the top ${buy} · +${schuur.maxProfit} florins`
  const alternative = alternatives.length === 0 ? '' : ` (or ${alternatives.join(', ')})`
  const hidden = Math.max(0, schuur.boxPrices.length - MAX_SHOWN_BOXES)
  return (
    <figure className={styles.pile} aria-label={title}>
      <figcaption className={styles.title}>{title}</figcaption>
      {schuur.boxPrices.length === 0 ? (
        <div className={styles.empty}>empty</div>
      ) : (
        <ol className={styles.boxes}>
          {schuur.boxPrices.slice(0, MAX_SHOWN_BOXES).map((price, index) => (
            <li key={index} className={boxClass(price, index < buy)}>
              {price}
            </li>
          ))}
        </ol>
      )}
      {hidden > 0 && (
        <div className={styles.more}>
          … and {hidden} more (<a href={fileUrl} download>see the file</a>)
        </div>
      )}
      <div className={styles.summary}>
        {summary}
        {alternative}
      </div>
      <div className={styles.downloads}>
        <a href={resultUrl} download>
          Download result
        </a>
      </div>
    </figure>
  )
}
