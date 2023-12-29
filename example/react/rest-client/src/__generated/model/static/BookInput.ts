export interface BookInput {
    readonly id?: number | undefined;
    readonly name: string;
    readonly edition: number;
    readonly price: number;
    readonly storeId?: number | undefined;
    readonly authorIds: ReadonlyArray<number>;
}
