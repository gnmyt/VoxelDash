import React, {useState, useRef, useEffect, useContext} from 'react';
import {Input} from "@/components/ui/input";
import {ScrollArea} from "@/components/ui/scroll-area";
import {Button} from "@/components/ui/button";
import {SocketContext} from "@/contexts/SocketContext";
import {postRequest} from "@/lib/RequestUtil";
import {parseAnsi, stripAnsi} from "@/lib/AnsiUtil";
import {PaperPlaneRightIcon, TerminalWindowIcon} from "@phosphor-icons/react";

interface LogEntry {
    text: string;
    color: string;
}

interface SocketMessage {
    data: string;
    event: string;
    message: string;
}

const Console = () => {
    const {lastMessage, attachEventListener, detachEventListener} = useContext(SocketContext)!;
    const [log, setLog] = useState<LogEntry[]>([]);
    const [input, setInput] = useState<string>('');
    const bottomRef = useRef<HTMLDivElement>(null);

    const [history, setHistory] = useState<string[]>(() => {
        const saved = localStorage.getItem("consoleHistory");
        return saved ? JSON.parse(saved) : [];
    });

    const [historyIndex, setHistoryIndex] = useState<number>(-1);

    useEffect(() => {
        bottomRef.current?.scrollIntoView({behavior: 'smooth'});
    }, [log]);

    useEffect(() => {
        attachEventListener('CONSOLE');
        return () => detachEventListener('CONSOLE');
    }, []);

    useEffect(() => {
        if (lastMessage) {
            try {
                const data = JSON.parse(lastMessage.data) as SocketMessage;
                if (data.event === 'CONSOLE') {
                    const message = data.message;
                    setLog((prev) => [...prev, {text: message, color: getLogColor(message)}]);
                }
            } catch (e) {
                console.error(e);
            }
        }
    }, [lastMessage]);

    const getLogColor = (message: string): string => {
        const stripped = stripAnsi(message);
        if (stripped.includes('[ERROR]')) return 'text-red-500';
        if (stripped.includes('[WARN') || stripped.includes('[WARNING]')) return 'text-amber-500';
        if (stripped.includes('[INFO]')) return 'text-primary';
        return 'text-foreground';
    };

    const renderLogEntry = (entry: LogEntry, index: number) => {
        const segments = parseAnsi(entry.text);
        const hasCustomColors = segments.some(s => Object.keys(s.style).length > 0);

        if (hasCustomColors) {
            return (
                <div key={index} className="break-all py-0.5">
                    {segments.map((segment, i) => (
                        <span key={i} style={segment.style}>{segment.text}</span>
                    ))}
                </div>
            );
        }

        return (
            <div key={index} className={`${entry.color} break-all py-0.5`}>
                {stripAnsi(entry.text)}
            </div>
        );
    };

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        if (input.trim()) {
            const newHistory = [...history, input];
            if (newHistory.length > 25) newHistory.shift();
            setHistory(newHistory);
            localStorage.setItem("consoleHistory", JSON.stringify(newHistory));

            setInput("");
            setHistoryIndex(-1);
            postRequest("action/command", {command: input});
        }
    };

    const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
        if (history.length === 0) return;

        if (e.key === "ArrowUp") {
            e.preventDefault();
            const newIndex = historyIndex < history.length - 1 ? historyIndex + 1 : historyIndex;
            setHistoryIndex(newIndex);
            setInput(history[history.length - 1 - newIndex] || '');
        } else if (e.key === "ArrowDown") {
            e.preventDefault();
            const newIndex = historyIndex > 0 ? historyIndex - 1 : -1;
            setHistoryIndex(newIndex);
            setInput(newIndex === -1 ? '' : history[history.length - 1 - newIndex]);
        }
    };

    return (
        <div className="flex flex-col p-6 pt-0 gap-4" style={{ height: 'calc(var(--app-vh) - 5.5rem)' }}>
            <div className="bg-card border rounded-xl font-mono p-6 flex-1 flex flex-col overflow-hidden">
                <ScrollArea className="flex-1">
                    <div className="pr-4 text-sm leading-relaxed">
                        {log.length === 0 && (
                            <div className="text-muted-foreground text-center py-8">
                                No console output yet. Server logs will appear here.
                            </div>
                        )}
                        {log.map((entry, index) => renderLogEntry(entry, index))}
                        <div ref={bottomRef}/>
                    </div>
                </ScrollArea>
            </div>
            <form onSubmit={handleSubmit} className="shrink-0">
                <div className="flex items-center gap-3">
                    <div className="flex-1 relative">
                        <TerminalWindowIcon className="absolute left-4 top-1/2 -translate-y-1/2 h-5 w-5 text-muted-foreground" />
                        <Input
                            type="text"
                            value={input}
                            onChange={(e) => setInput(e.target.value)}
                            onKeyDown={handleKeyDown}
                            className="h-14 pl-12 pr-4 text-base rounded-xl border bg-card font-mono"
                            placeholder="Enter command..."
                            autoFocus
                        />
                    </div>
                    <Button 
                        type="submit" 
                        size="lg" 
                        className="h-14 px-6 rounded-xl text-base"
                        disabled={!input.trim()}
                    >
                        <PaperPlaneRightIcon className="h-5 w-5 mr-2" weight="fill" />
                        Send
                    </Button>
                </div>
            </form>
        </div>
    );
};

export default Console;