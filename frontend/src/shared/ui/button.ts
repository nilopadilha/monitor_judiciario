import { cva, type VariantProps } from 'class-variance-authority'

/** Variantes no padrão ShadCN Vue, com os tokens do design system. Altura padrão de 44 px (alvo de toque). */
export const buttonVariants = cva(
  'inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-md font-medium transition-colors cursor-pointer disabled:cursor-not-allowed disabled:opacity-60 [&_svg]:size-4 [&_svg]:shrink-0',
  {
    variants: {
      variant: {
        default: 'bg-primary text-primary-foreground border border-primary hover:bg-primary/90',
        outline: 'bg-card text-foreground border border-input hover:bg-accent',
        ghost: 'text-foreground hover:bg-accent',
        destructive: 'bg-card text-destructive border border-destructive hover:bg-destructive/5',
        link: 'text-primary underline underline-offset-4 h-auto px-0',
      },
      size: {
        default: 'h-11 px-4 text-[15px]',
        sm: 'h-9 px-3 text-[13px]',
        icon: 'h-11 w-11',
      },
    },
    defaultVariants: { variant: 'default', size: 'default' },
  },
)

export type ButtonVariants = VariantProps<typeof buttonVariants>
