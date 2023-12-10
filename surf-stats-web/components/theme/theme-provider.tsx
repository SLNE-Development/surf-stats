export function ThemeProvider({
	children,
}: Readonly<{ children: React.ReactNode }>) {
	return <div className="bg-slate-800 text-white">{children}</div>;
}
