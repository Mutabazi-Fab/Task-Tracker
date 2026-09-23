import { TextField } from '../../../components/ui/TextField'

interface SourceDetailFieldProps {
  value: string
  onChange: (value: string) => void
  label: string
  disabled?: boolean
}

/** The free-text "Source Detail" field — always typed by hand, no clickable suggestions and
 *  no way to save one, so the value on a task is always exactly what the person meant to
 *  write rather than something they clicked without checking. */
export function SourceDetailField({ value, onChange, label, disabled }: SourceDetailFieldProps) {
  return (
    <TextField
      label={label}
      value={value}
      onChange={onChange}
      placeholder="e.g. Director Maj. Musoni, GPO, E&Y, Board of Directors"
      disabled={disabled}
    />
  )
}
