import { styled, useTheme } from '@mui/material/styles';
import Box from '@mui/material/Box';
import Drawer from '@mui/material/Drawer';
import CssBaseline from '@mui/material/CssBaseline';
import MuiAppBar from '@mui/material/AppBar';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import Divider from '@mui/material/Divider';
import IconButton from '@mui/material/IconButton';
import MenuIcon from '@mui/icons-material/Menu';
import ChevronLeftIcon from '@mui/icons-material/ChevronLeft';
import ChevronRightIcon from '@mui/icons-material/ChevronRight';
import { Menu } from './Menu';
import { Content } from './Content';
import { Stack, useMediaQuery } from '@mui/material';
import { TenantProvider } from './TenantContext';
import { WhiteTextField } from './WhiteTextField';
import { ChangeEvent, useCallback, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';

const drawerWidth = 240;

const Main = styled('main', { 
    shouldForwardProp: (prop) => prop !== 'open' 
})<{
    readonly open?: boolean;
}>(({ theme, open }) => ({
    flexGrow: 1,
    padding: theme.spacing(3),
    [theme.breakpoints.up("md")]: {
        transition: theme.transitions.create('margin', {
            easing: theme.transitions.easing.sharp,
            duration: theme.transitions.duration.leavingScreen,
        }),
        marginLeft: `-${drawerWidth}px`,
        ...(open && {
            transition: theme.transitions.create('margin', {
                easing: theme.transitions.easing.easeOut,
                duration: theme.transitions.duration.enteringScreen,
            }),
            marginLeft: 0,
        })
    }
}));

const AppBar = styled(MuiAppBar, {
  shouldForwardProp: (prop) => prop !== 'open',
})<{
    readonly open?: boolean;
}>(({ theme, open }) => ({
    [theme.breakpoints.up("md")]: {
        transition: theme.transitions.create(['margin', 'width'], {
            easing: theme.transitions.easing.sharp,
            duration: theme.transitions.duration.leavingScreen,
        }),
        ...(open && {
            width: `calc(100% - ${drawerWidth}px)`,
            marginLeft: `${drawerWidth}px`,
            transition: theme.transitions.create(['margin', 'width'], {
                easing: theme.transitions.easing.easeOut,
                duration: theme.transitions.duration.enteringScreen,
            }),
        }),
    }
}));

const DrawerHeader = styled('div')(({ theme }) => ({
    display: 'flex',
    alignItems: 'center',
    padding: theme.spacing(0, 1),
    ...theme.mixins.toolbar,
    justifyContent: 'flex-end',
}));

export function Home() {
    
    const theme = useTheme();

    const md = useMediaQuery(theme.breakpoints.up('md'));
    const [open, setOpen] = useState(md);

    const handleDrawerOpen = () => {
        setOpen(true);
    };

    const handleDrawerClose = () => {
        setOpen(false);
    };

    const queryClient = useQueryClient();

    const [tenant, setTenant] = useState<string | undefined>(() => {
        const tenant = (window as any).__tenant;
        return tenant !== undefined && tenant !== "" ? tenant : undefined;
    });

    const onTenantChange = useCallback((e: ChangeEvent<HTMLInputElement>) => {
        let tenant: string | undefined = e.target.value;
        if (tenant === "") {
            tenant = undefined;
        }
        setTenant(tenant);
        (window as any).__tenant = tenant; // Used by `ApiInstance.ts`
        queryClient.invalidateQueries();
    }, [queryClient]);

    return (
        <Box sx={{ display: 'flex' }}>
            <CssBaseline />
            <AppBar position="fixed" open={open}>
                <Toolbar>
                    <IconButton
                        color="inherit"
                        aria-label="open drawer"
                        onClick={handleDrawerOpen}
                        edge="start"
                        sx={{ mr: 2, ...(open && { display: 'none' }) }}
                    >
                        <MenuIcon />
                    </IconButton>
                    <Stack direction="row" spacing={2} style={{width: '100%'}}>
                        <Typography variant="h6" noWrap component="div" flex={1}>
                            Jimmer example REST client
                        </Typography>
                        <WhiteTextField 
                        label="Global tenant" 
                        size="small"
                        value={tenant}
                        onChange={onTenantChange}/>
                    </Stack>
                </Toolbar>
            </AppBar>
            <Drawer
                sx={{
                    width: drawerWidth,
                    flexShrink: 0,
                    '& .MuiDrawer-paper': {
                        width: drawerWidth,
                        boxSizing: 'border-box',
                    },
                }}
                variant={md ? "persistent" : "temporary"}
                anchor="left"
                open={open}
                onClose={handleDrawerClose}>
                <DrawerHeader>
                    <IconButton onClick={handleDrawerClose}>
                        {theme.direction === 'ltr' ? <ChevronLeftIcon /> : <ChevronRightIcon />}
                    </IconButton>
                </DrawerHeader>
                <Divider />
                <Menu/>
            </Drawer>
            <Main open={open}>
                <DrawerHeader />
                <TenantProvider value={tenant}>
                    <Content/>
                </TenantProvider>
            </Main>
        </Box>
    );
}
