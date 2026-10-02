import type { ChangeEvent } from 'react'

/** A button that picks one .txt or .json file. */
export function FilePicker({
  label,
  disabled = false,
  onFile,
}: {
  readonly label: string
  readonly disabled?: boolean
  readonly onFile: (file: File) => void
}) {
  const pick = (event: ChangeEvent<HTMLInputElement>) => {
    const [file] = event.target.files ?? []
    event.target.value = ''
    if (file) {
      onFile(file)
    }
  }
  return (
    <label className="button">
      {label}
      <input type="file" accept=".txt,.json" className="visually-hidden" disabled={disabled} onChange={pick} />
    </label>
  )
}
