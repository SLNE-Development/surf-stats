"use client";

import { useRef, useState } from "react";
import { Button } from "../components/ui/button";

export default function Home() {
	const [cookieBooster, setCookieBooster] = useState(false);
	const [counter, setCounter] = useState(0);
	const buttonRef = useRef<HTMLButtonElement>(null);

	return (
		<div className="flex flex-col gap-2 items-center pt-2 h-full">
			<div className="absolute bottom-1 right-1">
				<Button
					ref={buttonRef}
					variant={"warning"}
					size={"sm"}
					className="gap-2"
					onClick={() => {
						setCookieBooster(true);
						buttonRef.current!.disabled = true;

						setTimeout(() => {
							setCookieBooster(false);
							buttonRef.current!.disabled = false;
						}, 2 * 1000);
					}}
				>
					<i className="fas fa-cookie"></i>
					<div className="h-full bg-black w-1"></div>
					<i className="fas fa-rocket"></i>
				</Button>
			</div>

			<span className="bg-sky-500 px-4 py-2 rounded w-1/2 text-center shadow">
				{counter}
			</span>
			<Button
				variant={"info"}
				size={"lg"}
				className="gap-2 w-1/2"
				onClick={() => {
					setCounter(counter + (cookieBooster ? 10 : 1));
				}}
			>
				<i className="fas fa-cookie"></i>
				<span>+1</span>
			</Button>
		</div>
	);
}
