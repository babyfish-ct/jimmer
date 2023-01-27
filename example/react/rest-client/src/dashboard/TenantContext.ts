import { createContext, useContext } from "react";

const tenantContext = createContext<string | undefined>(undefined);

export const TenantProvider = tenantContext.Provider;

export function useTenant(): string | undefined {
    return useContext(tenantContext);
}