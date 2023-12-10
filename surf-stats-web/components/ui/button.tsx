import { cva, type VariantProps } from "class-variance-authority";
import * as React from "react";

import { cn } from "@/lib/utils";

const buttonVariants = cva(
	"inline-flex items-center justify-center whitespace-nowrap rounded font-medium transition-colors duration-150 disabled:pointer-events-none disabled:opacity-50",
	{
		variants: {
			variant: {
				default:
					"bg-primary-500 text-white hover:bg-primary-500/80 shadow",
				success:
					"bg-emerald-500 text-white hover:bg-emerald-500/80 shadow",
				warning:
					"bg-yellow-500 text-black hover:bg-yellow-500/80 shadow",
				info: "bg-sky-500 text-white hover:bg-sky-500/80 shadow",
				danger: "bg-red-600 text-white hover:bg-red-600/80 shadow",
				outline:
					"border border-white bg-background hover:bg-primary-500 hover:text-white shadow",
				secondary:
					"bg-gray-500 text-secondary-foreground hover:bg-gray-500/80 shadow",
				ghost: "hover:bg-primary-500 hover:text-white hover:shadow",
				link: "text-primary underline-offset-4 hover:underline",
			},
			size: {
				default: "h-10 px-4 py-2",
				sm: "h-9 px-3",
				lg: "h-11 px-8",
				icon: "h-10 w-10",
			},
		},
		defaultVariants: {
			variant: "default",
			size: "default",
		},
	}
);

export interface ButtonProps
	extends React.ButtonHTMLAttributes<HTMLButtonElement>,
		VariantProps<typeof buttonVariants> {
	asChild?: boolean;
}

const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
	({ className, variant, size, asChild = false, ...props }, ref) => {
		return (
			<button
				className={cn(buttonVariants({ variant, size, className }))}
				ref={ref}
				{...props}
			/>
		);
	}
);
Button.displayName = "Button";

export { Button, buttonVariants };
