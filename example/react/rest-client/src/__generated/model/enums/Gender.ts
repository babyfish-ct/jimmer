export const Gender_CONSTANTS = [
    'MALE', 
    'FEMALE'
] as const;
export type Gender = typeof Gender_CONSTANTS[number];
