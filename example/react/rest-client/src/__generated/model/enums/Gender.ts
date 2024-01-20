export const Gender_CONSTANTS = [
    'MALE', 
    'FEMALE'
] as const;
export const Gender_CONSTANT_MAP = {
    "Male": 'MALE', 
    "Female": 'FEMALE'
} as const;
export type Gender = typeof Gender_CONSTANTS;