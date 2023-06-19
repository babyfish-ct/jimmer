import React, { useCallback, useState } from "react";
import {
	G2,
	Chart,
	Tooltip,
	Interval,
	Interaction,
    Axis,
    Legend
} from "bizcharts";

import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';
import FormGroup from '@mui/material/FormGroup';
import FormControlLabel from '@mui/material/FormControlLabel';
import Checkbox from '@mui/material/Checkbox';
import Divider from '@mui/material/Divider';
import { ScaleOption } from "bizcharts/lib/interface";

export const Benchmark: React.FC<{
    readonly type: "OPS" | "TIME",
    readonly locale?: "zh"
}> = ({type, locale}) => {

    const [showJdbc, setShowJdbc] = useState(type !== "OPS");
    const onShowJdbcChange = useCallback((event: React.ChangeEvent<HTMLInputElement>, checked: boolean) => {
        setShowJdbc(checked);
    }, []);

    return (
        <Tabs groupId="chart-mode">
            <TabItem value="chart" label={locale === "zh" ? "图表" : "Chart"} default>
                { 
                    type === "OPS" && 
                    <>
                        <FormGroup>
                            <FormControlLabel 
                            control={<Checkbox value={showJdbc} onChange={onShowJdbcChange}/>} 
                            label={locale === "zh" ? "显示原生JDBC指标" : "Show native JDBC metrics"}/>
                        </FormGroup> 
                        <Divider/>
                    </>
                }
                {
                    type == "OPS" ?
                    <BenchmarkChart rows={opsRows} scale={opsScale} showJdbc={showJdbc}/> :
                    <BenchmarkChart rows={timeRows} scale={timeScale} showJdbc={showJdbc}/>
                }
            </TabItem>
            <TabItem value="data" label={locale === "zh" ? "数据" : "Data"}>
                {
                    type === "OPS" ?
                    <BenchmarkTable rows={sortedOpsRows} valueTitle="Ops/s"/> :
                    <BenchmarkTable rows={sortedTimeRows} valueTitle="Time(μs)"/>
                }
            </TabItem>
        </Tabs>
    );
};

const BenchmarkChart: React.FC<{
    readonly rows: ReadonlyArray<Row>,
    readonly scale: {
        readonly [field: string]: ScaleOption
    },
    readonly showJdbc: boolean
}> = ({rows, scale, showJdbc}) => {
    return (
        <Chart 
        filter={showJdbc ? undefined : {"framework": (v: any) => !(v as string).startsWith("JDBC")}}
        height={400} 
        padding="auto" 
        data={rows} 
        scale={scale} 
        autoFit>
			<Interval
				adjust={[
					{
						type: 'dodge',
						marginRatio: 0,
					},
				]}
				color="framework"
				position="dataCount*value"
			/>
            <Axis name="value" title/>
            <Legend position="left"/>
			<Tooltip shared />
			<Interaction type="active-region" />
		</Chart>
    );
}

const BenchmarkTable: React.FC<{readonly rows: ReadonlyArray<Row>, readonly valueTitle: string}> = ({rows, valueTitle}) => {
    return (
        <Table>
            <TableHead>
                <TableRow>
                    <TableCell>Framework</TableCell>
                    <TableCell>Data count</TableCell>
                    <TableCell>{valueTitle}</TableCell>
                </TableRow>
            </TableHead>
            <TableBody>
                {
                    rows.map(row => 
                        <TableRow key={`${row.framework}-${row.dataCount}`}>
                            <TableCell>{row.framework}</TableCell>
                            <TableCell>{row.dataCount}</TableCell>
                            <TableCell>{row.value}</TableCell>
                        </TableRow>
                    )
                }
            </TableBody>
        </Table>
    );
};

const opsScale = {
	dataCount: {
		alias: "Data count"
	},
	value: {
		alias: "Ops/s"
	}
};

const timeScale = {
	dataCount: {
		alias: "Data count"
	},
	value: {
		alias: 'Time(μs)'
	}
};

interface Row {
    readonly framework: string;
    readonly dataCount: string;
    readonly value: number;
};

const rows: ReadonlyArray<Row> = [
    {framework: "JDBC(ColIndex)", dataCount: "10", value: 656650},
	{framework: "JDBC(ColIndex)", dataCount: "20", value: 487951},
	{framework: "JDBC(ColIndex)", dataCount: "50", value: 261562},
	{framework: "JDBC(ColIndex)", dataCount: "100", value: 133467},
	{framework: "JDBC(ColIndex)", dataCount: "200", value: 70818},
	{framework: "JDBC(ColIndex)", dataCount: "500", value: 31787},
	{framework: "JDBC(ColIndex)", dataCount: "1000", value: 15925},
	{framework: "JDBC(ColName)", dataCount: "10", value: 339780},
	{framework: "JDBC(ColName)", dataCount: "20", value: 240816},
	{framework: "JDBC(ColName)", dataCount: "50", value: 128317},
	{framework: "JDBC(ColName)", dataCount: "100", value: 69694},
	{framework: "JDBC(ColName)", dataCount: "200", value: 37482},
	{framework: "JDBC(ColName)", dataCount: "500", value: 16280},
	{framework: "JDBC(ColName)", dataCount: "1000", value: 8200},
	{framework: "Jimmer(Java)", dataCount: "10", value: 309427},
	{framework: "Jimmer(Java)", dataCount: "20", value: 219976},
	{framework: "Jimmer(Java)", dataCount: "50", value: 111928},
	{framework: "Jimmer(Java)", dataCount: "100", value: 63463},
	{framework: "Jimmer(Java)", dataCount: "200", value: 33770},
	{framework: "Jimmer(Java)", dataCount: "500", value: 14089},
	{framework: "Jimmer(Java)", dataCount: "1000", value: 7236},
	{framework: "Jimmer(Kotlin)", dataCount: "10", value: 309306},
	{framework: "Jimmer(Kotlin)", dataCount: "20", value: 210098},
	{framework: "Jimmer(Kotlin)", dataCount: "50", value: 108242},
	{framework: "Jimmer(Kotlin)", dataCount: "100", value: 61745},
	{framework: "Jimmer(Kotlin)", dataCount: "200", value: 31390},
	{framework: "Jimmer(Kotlin)", dataCount: "500", value: 13438},
	{framework: "Jimmer(Kotlin)", dataCount: "1000", value: 6955},
    {framework: "MyBatis", dataCount: "10", value: 74459},
	{framework: "MyBatis", dataCount: "20", value: 45571},
	{framework: "MyBatis", dataCount: "50", value: 19030},
	{framework: "MyBatis", dataCount: "100", value: 10136},
	{framework: "MyBatis", dataCount: "200", value: 5250},
	{framework: "MyBatis", dataCount: "500", value: 2141},
	{framework: "MyBatis", dataCount: "1000", value: 1100},
	{framework: "EasyQuery", dataCount: "10", value: 212436},
	{framework: "EasyQuery", dataCount: "20", value: 141382},
	{framework: "EasyQuery", dataCount: "50", value: 72603},
	{framework: "EasyQuery", dataCount: "100", value: 41003},
	{framework: "EasyQuery", dataCount: "200", value: 21663},
	{framework: "EasyQuery", dataCount: "500", value: 9055},
	{framework: "EasyQuery", dataCount: "1000", value: 4567},
	{framework: "Exposed", dataCount: "10", value: 91146},
	{framework: "Exposed", dataCount: "20", value: 61239},
	{framework: "Exposed", dataCount: "50", value: 31164},
	{framework: "Exposed", dataCount: "100", value: 16868},
	{framework: "Exposed", dataCount: "200", value: 9231},
	{framework: "Exposed", dataCount: "500", value: 3754},
	{framework: "Exposed", dataCount: "1000", value: 1944},
	{framework: "JPA(Hibernate)", dataCount: "10", value: 100948},
	{framework: "JPA(Hibernate)", dataCount: "20", value: 59305},
	{framework: "JPA(Hibernate)", dataCount: "50", value: 25650},
	{framework: "JPA(Hibernate)", dataCount: "100", value: 13534},
	{framework: "JPA(Hibernate)", dataCount: "200", value: 6985},
	{framework: "JPA(Hibernate)", dataCount: "500", value: 2795},
	{framework: "JPA(Hibernate)", dataCount: "1000", value: 1421},
	{framework: "JPA(EclipseLink)", dataCount: "10", value: 63586},
	{framework: "JPA(EclipseLink)", dataCount: "20", value: 33266},
	{framework: "JPA(EclipseLink)", dataCount: "50", value: 13363},
	{framework: "JPA(EclipseLink)", dataCount: "100", value: 6762},
	{framework: "JPA(EclipseLink)", dataCount: "200", value: 3346},
	{framework: "JPA(EclipseLink)", dataCount: "500", value: 1344},
	{framework: "JPA(EclipseLink)", dataCount: "1000", value: 663},
	{framework: "JOOQ", dataCount: "10", value: 60528},
	{framework: "JOOQ", dataCount: "20", value: 34837},
	{framework: "JOOQ", dataCount: "50", value: 14994},
	{framework: "JOOQ", dataCount: "100", value: 8247},
	{framework: "JOOQ", dataCount: "200", value: 4640},
	{framework: "JOOQ", dataCount: "500", value: 1628},
	{framework: "JOOQ", dataCount: "1000", value: 880},
	{framework: "Nutz", dataCount: "10", value: 73807},
	{framework: "Nutz", dataCount: "20", value: 40285},
	{framework: "Nutz", dataCount: "50", value: 19109},
	{framework: "Nutz", dataCount: "100", value: 8724},
	{framework: "Nutz", dataCount: "200", value: 4300},
	{framework: "Nutz", dataCount: "500", value: 1882},
	{framework: "Nutz", dataCount: "1000", value: 945},
	{framework: "ObjectiveSQL", dataCount: "10", value: 58354},
	{framework: "ObjectiveSQL", dataCount: "20", value: 31492},
	{framework: "ObjectiveSQL", dataCount: "50", value: 11794},
	{framework: "ObjectiveSQL", dataCount: "100", value: 5522},
	{framework: "ObjectiveSQL", dataCount: "200", value: 3394},
	{framework: "ObjectiveSQL", dataCount: "500", value: 1327},
	{framework: "ObjectiveSQL", dataCount: "1000", value: 686},
	{framework: "Spring Data JDBC", dataCount: "10", value: 20668},
	{framework: "Spring Data JDBC", dataCount: "20", value: 10877},
	{framework: "Spring Data JDBC", dataCount: "50", value: 4287},
	{framework: "Spring Data JDBC", dataCount: "100", value: 2042},
	{framework: "Spring Data JDBC", dataCount: "200", value: 1101},
	{framework: "Spring Data JDBC", dataCount: "500", value: 456},
	{framework: "Spring Data JDBC", dataCount: "1000", value: 218},
	{framework: "Ktorm", dataCount: "10", value: 18956},
	{framework: "Ktorm", dataCount: "20", value: 9511},
	{framework: "Ktorm", dataCount: "50", value: 3965},
	{framework: "Ktorm", dataCount: "100", value: 2214},
	{framework: "Ktorm", dataCount: "200", value: 892},
	{framework: "Ktorm", dataCount: "500", value: 455},
	{framework: "Ktorm", dataCount: "1000", value: 211},
];

const opsRows = rows.map(item => ({...item, value: Math.round(item.value)}));

const timeRows = rows.map(item => ({...item, value: Math.round(1000000 / item.value)}));

function compareByDataCount(a: Row, b: Row) : number {
    return parseInt(a.dataCount) - parseInt(b.dataCount);
}

const sortedOpsRows = opsRows.sort(compareByDataCount);

const sortedTimeRows = timeRows.sort(compareByDataCount);

