export interface BookInput {
    readonly id?: number | null | undefined;
    readonly name: string;
    readonly edition: number;
    readonly price: number;
    readonly storeId?: number | null | undefined;
    readonly authorIds?: ReadonlyArray<number> | null | undefined;
}
