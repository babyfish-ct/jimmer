import { Divider, List, ListItem, ListItemButton, ListItemText } from "@mui/material";
import { FC, memo } from "react";
import { useLocation } from "wouter";

export const Menu: FC = memo(() => {

    const [location, setLocation] = useLocation();

    return (
        <List>
            {
                ITEMS.map(item => {
                    if (item.location === undefined) {
                        return <Divider key={item.key}/>;
                    }
                    return (
                        <ListItem key={item.key} disablePadding>
                            <ListItemButton 
                            selected={location === item.location}
                            onClick={() => setLocation(item.location!!)}>
                                <ListItemText primary={item.text} />
                            </ListItemButton>
                        </ListItem>
                    ); 
                })
            }
        </List>
    );
});

interface Item {
    readonly location?: string,
    readonly text?: string,
    readonly key?: string
}

const ITEMS: ReadonlyArray<Item> = [
    {location: "/simpleBookStores", text: "Simple BookStores"},
    {location: "/complexBookStores", text: "Complex BookStores"},
    {},
    {location: "/simpleBooks", text: "Simple Books"},
    {location: "/complexBooks", text: "Complex Books"},
    {},
    {location: "/simpleAuthors", text: "Simple Authors"},
    {location: "/complexAuthors", text: "Complex Authors"},
].map((item, index) => {
    return {...item, key: item.location ?? `divider-${index}`};
});