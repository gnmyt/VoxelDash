import {useState} from 'react'
import {Switch} from '@/components/ui/switch'
import {Input} from '@/components/ui/input'
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select'
import {ParsedProperty} from '@/types/config'
import {formatPropertyName} from '@/lib/PropertyUtil.ts'
import {exists, t} from "i18next";

interface PropertyCardProps {
    property: ParsedProperty;
    onValueChange: (name: string, value: string) => void;
}

export function PropertyCard({property, onValueChange}: PropertyCardProps) {
    const [value, setValue] = useState(property.value);
    const Icon = property.mapping.icon;

    const handleChange = (newValue: string) => {
        setValue(newValue);
        onValueChange(property.name, newValue);
    }

    const renderInput = () => {
        switch (property.mapping.type) {
            case 'boolean':
                return (
                    <div className="flex items-center gap-3">
                        <Switch 
                            checked={property.parsedValue as boolean}
                            onCheckedChange={(checked) => handleChange(checked.toString())}
                            className="data-[state=checked]:bg-primary"
                        />
                        <span className={`text-sm font-medium ${(property.parsedValue as boolean) ? 'text-primary' : 'text-muted-foreground'}`}>
                            {(property.parsedValue as boolean) ? t("properties.enabled") : t("properties.disabled")}
                        </span>
                    </div>
                )
            case 'number':
                return (
                    <Input
                        type="number"
                        value={value}
                        onChange={(e) => handleChange(e.target.value)}
                        className="w-full sm:w-48 h-12 text-base rounded-xl"
                    />
                )
            case 'gamemode':
            case 'difficulty':
                return (
                    <Select value={value} onValueChange={handleChange}>
                        <SelectTrigger className="w-full sm:w-48 h-12 text-base rounded-xl">
                            <SelectValue/>
                        </SelectTrigger>
                        <SelectContent className="rounded-xl">
                            {property.mapping.options?.map((option) => (
                                <SelectItem key={option} value={option} className="rounded-lg h-10 text-base">
                                    {option.charAt(0).toUpperCase() + option.slice(1)}
                                </SelectItem>
                            ))}
                        </SelectContent>
                    </Select>
                )
            case 'json':
                return (
                    <Input 
                        value={value} 
                        onChange={(e) => handleChange(e.target.value)}
                        className="flex-1 h-12 text-base rounded-xl font-mono"
                    />
                )
            default:
                return (
                    <Input 
                        value={value} 
                        onChange={(e) => handleChange(e.target.value)}
                        className="flex-1 max-w-md h-12 text-base rounded-xl"
                    />
                )
        }
    }

    return (
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 p-4 rounded-xl border bg-background hover:bg-accent/50 transition-colors">
            <div className="flex items-center gap-4 min-w-0">
                <div className="h-10 w-10 rounded-xl bg-muted flex items-center justify-center shrink-0">
                    <Icon className="h-5 w-5 text-muted-foreground"/>
                </div>
                <div className="min-w-0">
                    <h3 className="text-base font-medium truncate">
                        {formatPropertyName(property.name)}
                    </h3>
                    <p className="text-sm text-muted-foreground truncate">
                        {exists("properties." + property.name) ? t("properties." + property.name) : property.name}
                    </p>
                </div>
            </div>
            <div className="shrink-0 sm:ml-4 self-start sm:self-auto">
                {renderInput()}
            </div>
        </div>
    )
}

